package com.emop.pause.core.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.EvChargeState
import android.content.Context
import android.util.Log
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * AAOS 표준 차량 속성 (CarPropertyManager). Pleos Vehicle SDK에 없는 신호(속도·주차 브레이크·충전 상태)를 여기서 받는다.
 * API는 SDK `platforms/android-35/optional/android.car.jar`를 javap로 확인한 것만 쓴다 (2026-10-29):
 * `Car.createCar(Context)`, `Car.getCarManager(Car.PROPERTY_SERVICE)`, `CarPropertyManager.getCarPropertyConfig(int)`,
 * `getProperty(Class, int, int)`, `registerCallback(callback, int, float)`, `unregisterCallback(callback)`,
 * `CarPropertyEventCallback.onChangeEvent/onErrorEvent`. 속성 ID는 `VehiclePropertyIds` 상수.
 *
 * 권한: PERF_VEHICLE_SPEED = `CAR_SPEED`(dangerous), EV_BATTERY_LEVEL·EV_CHARGE_STATE = `CAR_ENERGY`(dangerous),
 * GEAR_SELECTION·PARKING_BRAKE_ON = `CAR_POWERTRAIN`(normal), INFO_EV_BATTERY_CAPACITY = `CAR_INFO`(normal).
 * 권한이 없거나 차가 지원하지 않으면 config가 null이거나 예외 → 그 신호만 미지원.
 * 속성은 모두 전역(areaId 0)이다.
 */
class AaosPropertySource(context: Context) : VehicleSource {
    private val appContext = context.applicationContext
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val _capabilities = MutableStateFlow<Set<Signal>>(emptySet())
    override val capabilities: StateFlow<Set<Signal>> = _capabilities.asStateFlow()
    private val latest = MutableStateFlow(SignalSample(timestampMs = 0))
    private var capacityWh: Float? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = apply(value)
        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "속성 오류 ${VehiclePropertyIds.toString(propertyId)}")
        }
    }

    fun start() {
        try {
            car = Car.createCar(appContext)
            manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        } catch (e: Exception) {
            Log.w(TAG, "차량 속성 서비스 연결 실패: ${e.javaClass.simpleName}")
            return
        }
        val m = manager ?: return
        capacityWh = read(m, VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)?.value as? Float
        for ((prop, signal) in PROPERTIES) {
            if (signal == Signal.BATTERY_SOC && capacityWh == null) continue
            try {
                if (m.getCarPropertyConfig(prop) == null) {
                    // 차가 지원하지 않거나 권한이 없으면 config가 null이다.
                    Log.i(TAG, "${VehiclePropertyIds.toString(prop)} 설정 없음 (미지원 또는 권한 없음)")
                    continue
                }
                read(m, prop)?.let(::apply)
                if (m.registerCallback(callback, prop, rateOf(prop))) _capabilities.value = _capabilities.value + signal
            } catch (e: Exception) {
                // 권한 없음(SecurityException)·미지원(IllegalArgumentException)은 그 신호만 뺀다.
                Log.w(TAG, "${VehiclePropertyIds.toString(prop)} 사용 불가: ${e.javaClass.simpleName}")
            }
        }
        Log.i(TAG, "AAOS 속성 지원: ${_capabilities.value}")
    }

    fun stop() {
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
        _capabilities.value = emptySet()
    }

    override fun samples(): Flow<SignalSample> = flow {
        while (true) {
            emit(latest.value.copy(timestampMs = System.currentTimeMillis()))
            delay(1_000)
        }
    }

    private fun read(m: CarPropertyManager, prop: Int): CarPropertyValue<*>? = try {
        m.getProperty(Any::class.java, prop, 0)?.takeIf { it.status == CarPropertyValue.STATUS_AVAILABLE }
    } catch (e: Exception) {
        null
    }

    private fun apply(v: CarPropertyValue<*>) {
        if (v.status != CarPropertyValue.STATUS_AVAILABLE) return
        val value = v.value
        latest.value = when (v.propertyId) {
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> latest.value.copy(speedKph = (value as? Float)?.let(AaosMapping::kph))
            VehiclePropertyIds.GEAR_SELECTION -> latest.value.copy(gear = (value as? Int)?.let(AaosMapping::gear))
            VehiclePropertyIds.PARKING_BRAKE_ON -> latest.value.copy(parkingBrakeOn = value as? Boolean)
            VehiclePropertyIds.EV_BATTERY_LEVEL -> latest.value.copy(batterySocPercent = AaosMapping.socPercent(value as? Float, capacityWh))
            VehiclePropertyIds.EV_CHARGE_STATE -> latest.value.copy(charging = (value as? Int)?.let(AaosMapping::charging))
            else -> latest.value
        }
    }

    private fun rateOf(prop: Int) =
        if (prop == VehiclePropertyIds.PERF_VEHICLE_SPEED) CarPropertyManager.SENSOR_RATE_NORMAL else CarPropertyManager.SENSOR_RATE_ONCHANGE

    private companion object {
        const val TAG = "AaosProperty"
        val PROPERTIES = listOf(
            VehiclePropertyIds.PERF_VEHICLE_SPEED to Signal.SPEED,
            VehiclePropertyIds.GEAR_SELECTION to Signal.GEAR,
            VehiclePropertyIds.PARKING_BRAKE_ON to Signal.PARKING_BRAKE,
            VehiclePropertyIds.EV_BATTERY_LEVEL to Signal.BATTERY_SOC,
            VehiclePropertyIds.EV_CHARGE_STATE to Signal.CHARGING,
        )
    }
}

/** AAOS 속성 값 → 앱 모델. 순수 함수 (상수는 컴파일 시 인라인되어 JVM 테스트에서 android.car 클래스를 불러오지 않는다). */
object AaosMapping {
    /** PERF_VEHICLE_SPEED는 m/s, 후진이면 음수. 앱은 절댓값 km/h. */
    fun kph(metersPerSec: Float): Float = kotlin.math.abs(metersPerSec) * 3.6f

    /** GEAR_SELECTION은 VehicleGear 비트값. 1~9단은 주행 기어. */
    fun gear(v: Int): Gear = when (v) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE
    }

    /** EV_BATTERY_LEVEL(Wh) / INFO_EV_BATTERY_CAPACITY(Wh). 용량을 모르면 null. */
    fun socPercent(levelWh: Float?, capacityWh: Float?): Int? {
        if (levelWh == null || capacityWh == null || capacityWh <= 0f) return null
        return kotlin.math.round(levelWh / capacityWh * 100f).toInt().coerceIn(0, 100)
    }

    /** EV_CHARGE_STATE: CHARGING만 충전 중. 완충·충전 안 함·오류는 false, 모름은 null. */
    fun charging(state: Int): Boolean? = when (state) {
        EvChargeState.STATE_CHARGING -> true
        EvChargeState.STATE_UNKNOWN -> null
        else -> false
    }
}
