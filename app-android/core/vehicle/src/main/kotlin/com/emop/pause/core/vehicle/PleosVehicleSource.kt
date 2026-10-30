package com.emop.pause.core.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
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
 * Pleos Vehicle SDK 2.3.2 어댑터. 시그니처는 aar(classes.jar)를 javap로 확인한 것만 쓴다 (M1, 2026-10-28).
 *
 * 확인한 API: Vehicle(context).initialize()/release(), getGear().registerGearSelection,
 * getEvBattery().registerEvBatteryStateOfCharge/checkEvBatteryStateOfChargeCapabilities,
 * getHVAC().registerHvacTemperature/registerHvacAcState/registerHvacRecirculationState(SeatArea, listener)와
 * 각 checkHvac*Capabilities, 초기값용 getHvacTemperature/getHvacAcState/getHvacRecirculationState/getEvBatteryStateOfCharge.
 * 리스너는 값이 바뀔 때만 오므로 등록 직후 get*으로 현재 값을 한 번 읽는다.
 *
 * 없는 신호: 속도(VehicleSpeedListener는 SDK에서 제거됨), 충전 중 여부(EvChargeState는 공개 API에 노출 안 됨).
 * 두 값은 null로 두고 엔진이 가중치를 재정규화한다. TODO(pleos-sdk): 대체 수단 결정 후 채운다.
 */
class PleosVehicleSource(context: Context) : VehicleSource {
    private val vehicle = Vehicle(context)
    /** capability 확인이 영역을 주지 않을 때만 쓰는 기본값. 실제로는 차가 알려준 지원 영역을 쓴다. */
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val areas = mutableMapOf<Signal, SeatArea>()

    private val _capabilities = MutableStateFlow<Set<Signal>>(emptySet())
    override val capabilities: StateFlow<Set<Signal>> = _capabilities.asStateFlow()

    private val latest = MutableStateFlow(SignalSample(timestampMs = 0))

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = update { it.copy(gear = gear?.toModel()) }
        override fun onFailed(e: Exception) = fail(Signal.GEAR, e)
    }
    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) = update { it.copy(batterySocPercent = value) }
        override fun onFailed(e: Exception) = fail(Signal.BATTERY_SOC, e)
    }
    // 실내 온도 = 측정값(`…TemperatureCurrent`). `getHvacTemperature`·`registerHvacTemperature`는 설정 온도라 쓰지 않는다
    // (javap Vehicle 2.3.2, 2026-10-30 확인 — 이전에는 설정 온도를 실내 온도로 읽고 있었다). 알림을 받으면 섭씨로 다시 읽는다
    // (리스너 값의 단위가 문서에 없어서).
    private val tempListener = object : HvacTemperatureListener {
        override fun onHvacTemperatureUpdated(temperature: Float?) {
            val area = areas[Signal.HVAC_TEMP] ?: return
            runCatching {
                vehicle.getHVAC().getHvacTemperatureCurrent(area, TemperatureUnit.CELSIUS, { v -> update { it.copy(hvacTempC = v) } }, { e -> warnRead(Signal.HVAC_TEMP, e) })
            }
        }
        override fun onFailed(e: Exception) = fail(Signal.HVAC_TEMP, e)
    }
    private val acListener = object : HvacAcStateListener {
        override fun onHvacAcStateUpdated(state: Boolean?) = update { it.copy(hvacAcOn = state) }
        override fun onFailed(e: Exception) = fail(Signal.HVAC_AC, e)
    }
    private val recirculationListener = object : HvacRecirculationStateListener {
        override fun onHvacAutoRecirculationStateUpdated(state: Boolean?) = update { it.copy(recirculationOn = state) }
        override fun onFailed(e: Exception) = fail(Signal.HVAC_RECIRCULATION, e)
    }

    // registerGearSelection은 SDK에서 deprecated("use the Google GEAR_SELECTION property").
    // TODO(pleos-sdk): CarPropertyManager GEAR_SELECTION으로 옮길지 결정 대기 (권한 협의 필요).
    @Suppress("DEPRECATION")
    fun start() {
        vehicle.initialize()
        // 기어는 capability 확인 API가 없어 등록 성공을 지원으로 본다 (실패 시 onFailed에서 제거).
        guard(Signal.GEAR) {
            addCapability(Signal.GEAR)
            vehicle.getGear().registerGearSelection(gearListener)
        }

        val battery = vehicle.getEvBattery()
        guard(Signal.BATTERY_SOC) {
            battery.checkEvBatteryStateOfChargeCapabilities({ cfg ->
                if (cfg.readable()) guard(Signal.BATTERY_SOC) {
                    addCapability(Signal.BATTERY_SOC)
                    battery.registerEvBatteryStateOfCharge(socListener)
                    battery.getEvBatteryStateOfCharge({ v -> update { it.copy(batterySocPercent = v) } }, { e -> warnRead(Signal.BATTERY_SOC, e) })
                }
            }, { e -> fail(Signal.BATTERY_SOC, e) })
        }

        val hvac = vehicle.getHVAC()
        guard(Signal.HVAC_TEMP) {
            hvac.checkHvacTemperatureCurrentCapabilities({ cfg, supported ->
                val area = supported.firstOrNull() ?: driverSeat
                if (cfg.readable()) guard(Signal.HVAC_TEMP) {
                    addCapability(Signal.HVAC_TEMP)
                    areas[Signal.HVAC_TEMP] = area
                    hvac.registerHvacTemperatureCurrent(area, tempListener)
                    hvac.getHvacTemperatureCurrent(area, TemperatureUnit.CELSIUS, { v -> update { it.copy(hvacTempC = v) } }, { e -> warnRead(Signal.HVAC_TEMP, e) })
                }
            }, { e -> fail(Signal.HVAC_TEMP, e) })
        }
        guard(Signal.HVAC_AC) {
            hvac.checkHvacAcCapabilities({ cfg, supported ->
                val area = supported.firstOrNull() ?: driverSeat
                if (cfg.readable()) guard(Signal.HVAC_AC) {
                    addCapability(Signal.HVAC_AC)
                    areas[Signal.HVAC_AC] = area
                    hvac.registerHvacAcState(area, acListener)
                    hvac.getHvacAcState(area, { v -> update { it.copy(hvacAcOn = v) } }, { e -> warnRead(Signal.HVAC_AC, e) })
                }
            }, { e -> fail(Signal.HVAC_AC, e) })
        }
        guard(Signal.HVAC_RECIRCULATION) {
            hvac.checkHvacRecirculationCapabilities({ cfg, supported ->
                val area = supported.firstOrNull() ?: driverSeat
                if (cfg.readable()) guard(Signal.HVAC_RECIRCULATION) {
                    addCapability(Signal.HVAC_RECIRCULATION)
                    areas[Signal.HVAC_RECIRCULATION] = area
                    hvac.registerHvacRecirculationState(area, recirculationListener)
                    hvac.getHvacRecirculationState(area, { v -> update { it.copy(recirculationOn = v) } }, { e -> warnRead(Signal.HVAC_RECIRCULATION, e) })
                }
            }, { e -> fail(Signal.HVAC_RECIRCULATION, e) })
        }
    }

    @Suppress("DEPRECATION")
    fun stop() {
        runCatching {
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener)
            val hvac = vehicle.getHVAC()
            areas[Signal.HVAC_TEMP]?.let { hvac.unregisterHvacTemperatureCurrent(it, tempListener) }
            areas[Signal.HVAC_AC]?.let { hvac.unregisterHvacAcState(it, acListener) }
            areas[Signal.HVAC_RECIRCULATION]?.let { hvac.unregisterHvacRecirculationState(it, recirculationListener) }
        }
        vehicle.release()
    }

    /** 리스너는 값이 바뀔 때만 오므로 최신 값을 1Hz로 내보낸다. */
    override fun samples(): Flow<SignalSample> = flow {
        while (true) {
            emit(latest.value.copy(timestampMs = System.currentTimeMillis()))
            delay(1_000)
        }
    }

    private fun update(change: (SignalSample) -> SignalSample) {
        latest.value = change(latest.value)
    }

    /** SDK는 권한이 없으면 onFailed 대신 SecurityException을 바로 던진다 (에뮬레이터 확인). 그 신호만 미지원으로 둔다. */
    private inline fun guard(signal: Signal, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail(signal, e)
        }
    }

    /** 초기값 읽기 실패는 일시적일 수 있으므로(예: 공조 전원 꺼짐) 지원 목록에서 빼지 않는다. */
    private fun warnRead(signal: Signal, e: Exception) {
        Log.w(TAG, "$signal 현재 값 읽기 실패: ${e.javaClass.simpleName} ${e.message}")
    }

    private fun addCapability(signal: Signal) {
        _capabilities.value = _capabilities.value + signal
    }

    private fun fail(signal: Signal, e: Exception) {
        Log.w(TAG, "$signal 사용 불가: ${e.javaClass.simpleName} ${e.message}")
        _capabilities.value = _capabilities.value - signal
    }

    private fun CapabilityConfig.readable() = accessType == AccessType.READ || accessType == AccessType.READ_WRITE

    private companion object {
        const val TAG = "PleosVehicle"

        fun VehicleGear.toModel(): Gear = when (this) {
            VehicleGear.GEAR_PARK -> Gear.PARK
            VehicleGear.GEAR_REVERSE -> Gear.REVERSE
            VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
            VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
            // GEAR_DRIVE와 수동 기어 1~9단은 모두 주행 기어
            else -> Gear.DRIVE
        }
    }
}
