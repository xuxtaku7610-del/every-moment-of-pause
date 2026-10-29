package com.emop.pause.uistate

/**
 * 화면 계약: 화면이 그리는 값만 담은 상태 (docs/ui_contract.md).
 * 엔진 상태 → 이 상태 변환은 앱의 `presentation.UiMapper`가 하고, 미리보기·발표 모드 장면은 [Fixtures]가 만든다.
 * 화면은 엔진을 모르고, 사용자 동작은 [UiEvent]로만 돌려보낸다.
 */

enum class ChipIcon { KID, BOLT, PAW, BABY, BED, COMMA, NONE }

data class ChipUi(val text: String, val icon: ChipIcon = ChipIcon.NONE)

data class FatigueUi(val score: Int, val tone: FatigueTone, val detail: String)

data class FamilyLine(val kind: AvatarKind, val name: String, val text: String)

sealed interface DriveContent {
    /** 다음 쉼표 제안 또는 가고 있는 쉼표. [asking]이면 버튼(여기서 쉴게요·다음 곳으로)과 음성 힌트를 보인다. */
    data class Next(
        val minutes: Int,
        val place: String,
        val placeSub: String?,
        val chips: List<ChipUi>,
        val asking: Boolean,
    ) : DriveContent

    /** 쉴 필요 없음 (UI_SPEC §4.7). */
    data class Calm(val line: String, val chips: List<ChipUi>) : DriveContent

    /** 피로 위험 (UI_SPEC §4.8). 거절 버튼 없음. */
    data class Urgent(val score: Int, val place: String, val placeSub: String, val km: String) : DriveContent
}

data class DriveUi(
    val meta: String?,
    val content: DriveContent,
    val fatigue: FatigueUi?,
    val family: List<FamilyLine>,
    val journey: JourneyUi?,
    val offline: Boolean = false,
    /** CarUxRestrictions가 RESTRICTED: 오른쪽 카드를 숨기고 가장 급한 한 줄만 칩으로 (UI_SPEC §4.1). */
    val restricted: Boolean = false,
    val voice: VoiceSheetState? = null,
    /** 경유지 추가·안내 결과 한 줄 ("경로에 안성휴게소를 넣었어요"). */
    val toast: String? = null,
    /** 재생 중인 라디오 한 줄. 주행 중이라 버튼 없이 표시만 (끄기는 음성). */
    val radio: RadioLineUi? = null,
)

/** "라디오 2/4 · 티끌 모아 태산". [draft]면 "DRAFT" 표시 (디버그 빌드만 초안 재생). */
data class RadioLineUi(val title: String, val index: Int, val count: Int, val draft: Boolean)

/** 원정대 미션 한 줄 (멈춤 화면). */
data class MissionUi(val id: String, val title: String, val instructions: String, val done: Boolean, val draft: Boolean)

data class MemberUi(val id: String, val label: String, val kind: AvatarKind, val selected: Boolean)

data class PlanStopUi(val name: String, val facilities: String, val eta: String)

data class PrepareScreenUi(
    val meta: String?,
    val members: List<MemberUi>,
    /** 4..8, 8은 "8시간+". null = 아직 안 고름. */
    val sleepHours: Int?,
    val proactive: Boolean,
    val plan: List<PlanStopUi>,
    val journey: JourneyUi?,
    val rationale: String,
)

data class BatteryUi(val percent: Int, val target: Int, val power: String?, val doneAt: String?)

enum class ActivityKind { NAP, EXPEDITION, PET_WALK, FOCUS, STRETCH }

data class ActivityUi(
    val kind: ActivityKind,
    val who: String,
    val title: String,
    val description: String,
    val action: String,
    val recommended: Boolean,
    val enabled: Boolean = true,
    val badge: String? = null,
)

data class PauseScreenUi(
    val meta: String?,
    val lead: String,
    val number: String,
    val unit: String,
    val battery: BatteryUi?,
    /** 충전이 아닌 정차: 사용자가 10/15/20분 고름 (UI_SPEC §4.3). */
    val restChoice: Int?,
    val activities: List<ActivityUi>,
    val timeline: List<TimelinePoint>,
    val nowFraction: Float,
    val notice: String? = null,
    /** 원정대 미션 (아이 동승 시). 다 하면 [radioUnlocked]. */
    val missions: List<MissionUi> = emptyList(),
    val radioUnlocked: Boolean = false,
)

data class NapUi(val remainingSec: Int, val totalSec: Int, val wakeLine: String, val soundOn: Boolean, val waking: Boolean = false)

data class ReportStopUi(val name: String, val tags: List<String>, val arrived: String, val stayed: String)

data class ReportUi(
    val meta: String?,
    /** 쉼표 자리에서 끊은 문장 조각. 조각 사이에 rest 색 쉼표를 넣는다. */
    val sentence: List<String>,
    val distanceKm: Int,
    val stops: Int,
    val longestMin: Int,
    val log: List<ReportStopUi>,
    /** 피로 흐름(0~100), 시간 순 균등 간격. */
    val fatigueSeries: List<Int>,
    /** 쉼표 위치(시리즈 인덱스 비율 0~1). */
    val restMarks: List<Float>,
    val tip: String,
)

enum class OnboardStep { CONCEPT, MEMBERS, MIC }

data class SignalRowUi(val name: String, val inUse: Boolean, val source: String? = null)

enum class SettingsSection { SUGGEST, FAMILY, SIGNALS, PRIVACY, ABOUT }

data class SettingsUi(
    val section: SettingsSection = SettingsSection.SUGGEST,
    val timing: Int = 1,
    val voiceSuggest: Boolean = true,
    val kidAnswer: Boolean = true,
    val napSound: Boolean = false,
    val reduceMotion: Boolean = false,
    val signals: List<SignalRowUi> = emptyList(),
    val members: List<MemberUi> = emptyList(),
    val version: String = "",
)

// ───────────── 화면 공통 값 (예전 ui/components에서 옮김) ─────────────

enum class FatigueTone { CALM, CAUTION, ALERT }

fun toneOf(score: Int): FatigueTone = when {
    score >= 70 -> FatigueTone.ALERT
    score >= 40 -> FatigueTone.CAUTION
    else -> FatigueTone.CALM
}

fun toneLabel(tone: FatigueTone) = when (tone) {
    FatigueTone.CALM -> "편안"
    FatigueTone.CAUTION -> "주의"
    FatigueTone.ALERT -> "위험"
}

enum class AvatarKind { ADULT, KID, PET, CHARGE, NAP }

enum class StopMark { NEXT, UPCOMING, PAST }

data class JourneyStop(val fraction: Float, val label: String, val mark: StopMark)

/** 경로선 한 줄의 상태. fraction은 모두 경로상 거리 비율(0~1). */
data class JourneyUi(
    val progress: Float?,
    val stops: List<JourneyStop>,
    val start: String? = null,
    val end: String? = null,
)

enum class TimelineDot { PAST, NOW, ALARM, END }

data class TimelinePoint(val fraction: Float, val time: String, val label: String, val dot: TimelineDot)

enum class VoiceSheetState { LISTENING, FAILED }
