package com.emop.pause.core.model

/** 한 공급원이 지금 가진 값과 지원 신호. */
data class SourceReading(val source: SignalSource, val capabilities: Set<Signal>, val sample: SignalSample)

/** 합친 결과: 신호마다 실제로 쓴 공급원. 목록에 없는 신호는 [SignalSource.NONE]. */
data class MergedSignals(val sample: SignalSample, val sources: Map<Signal, SignalSource>) {
    val capabilities: Set<Signal> get() = sources.filterValues { it != SignalSource.NONE }.keys
    fun sourceOf(signal: Signal): SignalSource = sources[signal] ?: SignalSource.NONE
}

/**
 * 여러 공급원(Pleos SDK, AAOS 속성, 시뮬레이터)을 신호 단위로 합친다 — 순수 함수 (architecture §3.1 신호 수급 전략).
 * [readings]의 순서가 우선순위다. 신호마다 그 신호를 지원한다고 알린 첫 공급원의 값을 쓰고,
 * 지원은 하지만 아직 값이 null이면 다음 공급원으로 넘어가지 않는다(같은 신호를 두 출처에서 섞지 않는다).
 */
object SignalMerge {
    fun merge(readings: List<SourceReading>, timestampMs: Long): MergedSignals {
        val sources = Signal.entries.associateWith { sig ->
            readings.firstOrNull { sig in it.capabilities }?.source ?: SignalSource.NONE
        }
        fun pick(sig: Signal): SignalSample? = readings.firstOrNull { it.source == sources[sig] && sig in it.capabilities }?.sample
        val sample = SignalSample(
            timestampMs = timestampMs,
            speedKph = pick(Signal.SPEED)?.speedKph,
            gear = pick(Signal.GEAR)?.gear,
            parkingBrakeOn = pick(Signal.PARKING_BRAKE)?.parkingBrakeOn,
            batterySocPercent = pick(Signal.BATTERY_SOC)?.batterySocPercent,
            charging = pick(Signal.CHARGING)?.charging,
            hvacTempC = pick(Signal.HVAC_TEMP)?.hvacTempC,
            hvacAcOn = pick(Signal.HVAC_AC)?.hvacAcOn,
            recirculationOn = pick(Signal.HVAC_RECIRCULATION)?.recirculationOn,
            doors = pick(Signal.DOOR)?.doors,
        )
        return MergedSignals(sample, sources)
    }
}
