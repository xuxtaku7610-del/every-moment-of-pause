package com.emop.pause.core.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
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
 * 각 checkHvac*Capabilities.
 *
 * 없는 신호: 속도(VehicleSpeedListener는 SDK에서 제거됨), 충전 중 여부(EvChargeState는 공개 API에 노출 안 됨).
 * 두 값은 null로 두고 엔진이 가중치를 재정규화한다. TODO(pleos-sdk): 대체 수단 결정 후 채운다.
 */
class PleosVehicleSource(context: Context) : VehicleSource {
    private val vehicle = Vehicle(context)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)

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
    private val tempListener = object : HvacTemperatureListener {
        override fun onHvacTemperatureUpdated(temperature: Float?) = update { it.copy(hvacTempC = temperature) }
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
        addCapability(Signal.GEAR)
        vehicle.getGear().registerGearSelection(gearListener)

        val battery = vehicle.getEvBattery()
        battery.checkEvBatteryStateOfChargeCapabilities({ cfg ->
            if (cfg.readable()) {
                addCapability(Signal.BATTERY_SOC)
                battery.registerEvBatteryStateOfCharge(socListener)
            }
        }, { e -> fail(Signal.BATTERY_SOC, e) })

        val hvac = vehicle.getHVAC()
        hvac.checkHvacTemperatureCapabilities({ cfg, _, _ ->
            if (cfg.readable()) {
                addCapability(Signal.HVAC_TEMP)
                hvac.registerHvacTemperature(driverSeat, tempListener)
            }
        }, { e -> fail(Signal.HVAC_TEMP, e) })
        hvac.checkHvacAcCapabilities({ cfg, _ ->
            if (cfg.readable()) {
                addCapability(Signal.HVAC_AC)
                hvac.registerHvacAcState(driverSeat, acListener)
            }
        }, { e -> fail(Signal.HVAC_AC, e) })
        hvac.checkHvacRecirculationCapabilities({ cfg, _ ->
            if (cfg.readable()) {
                addCapability(Signal.HVAC_RECIRCULATION)
                hvac.registerHvacRecirculationState(driverSeat, recirculationListener)
            }
        }, { e -> fail(Signal.HVAC_RECIRCULATION, e) })
    }

    @Suppress("DEPRECATION")
    fun stop() {
        runCatching {
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener)
            val hvac = vehicle.getHVAC()
            hvac.unregisterHvacTemperature(driverSeat, tempListener)
            hvac.unregisterHvacAcState(driverSeat, acListener)
            hvac.unregisterHvacRecirculationState(driverSeat, recirculationListener)
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
