package com.emop.pause.uistate

/**
 * 화면 계약: 화면이 그리는 값만 담은 상태 (docs/ui_contract.md).
 * 엔진 상태 → 이 상태 변환은 앱의 `presentation.UiMapper`가 하고, 미리보기·발표 모드 장면은 [Fixtures]가 만든다.
 * 화면은 엔진을 모르고, 사용자 동작은 [UiEvent]로만 돌려보낸다. 디자인(v5)이 오면 화면(Compose)만 갈아 끼운다.
 */

/** 멀미 예보 단계 (화면 색·아이콘). 의학적 진단이 아닌 "주의 예보". */
enum class MotionTone { CALM, CAUTION, HIGH }

fun motionTone(index: Int): MotionTone = when {
    index >= 70 -> MotionTone.HIGH
    index >= 40 -> MotionTone.CAUTION
    else -> MotionTone.CALM
}

fun motionLabel(t: MotionTone) = when (t) {
    MotionTone.CALM -> "편안"
    MotionTone.CAUTION -> "주의"
    MotionTone.HIGH -> "높음"
}

/** "콩이 멀미 주의 45". */
data class MotionUi(val index: Int, val tone: MotionTone, val label: String)

// ───────────── 주행 (F1·F2, 주행 중 제한: 텍스트 2줄·버튼 2개·스크롤 없음) ─────────────

sealed interface DriveContent {
    /** 다음 쉼표 안내 한 줄 ("다음 쉼표는 32분 뒤 횡성휴게소예요"). 버튼 없음. */
    data class Calm(val line: String) : DriveContent

    /** 멀미 예보 한 줄 ("3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요"). 버튼 없음. */
    data class Forecast(val line: String) : DriveContent

    /**
     * 쉼표 추천. [reasons]는 실제 기여 상위 1~2개 ("콩이 멀미가 쌓였어요", "반려견 놀이터").
     * 버튼 2개(여기서 쉴게요·나중에) + 음성 힌트. [urgent](운전자 피로 위험)면 거절 버튼 없이 1개.
     */
    data class Suggest(val minutes: Int, val place: String, val reasons: List<String>, val urgent: Boolean = false) : DriveContent

    /** 수락해서 가고 있는 쉼표. 버튼 없음. */
    data class Heading(val minutes: Int, val place: String) : DriveContent
}

data class DriveUi(
    /** "강릉까지 1시간 10분". */
    val meta: String?,
    val motion: MotionUi?,
    val content: DriveContent,
    val voice: VoiceSheetState? = null,
    /** 결과 한 줄 ("경로에 횡성휴게소를 넣었어요"). */
    val toast: String? = null,
    /** CarUxRestrictions RESTRICTED: 가장 급한 한 줄만 (DS-03). */
    val restricted: Boolean = false,
    /** 서버 없이 내장 데이터로 동작 중. */
    val offline: Boolean = false,
)

enum class VoiceSheetState { LISTENING, FAILED }

// ───────────── 출발 체크 (F6) ─────────────

enum class CheckKind { HARNESS, WATER, POTTY_BAGS }

data class CheckUi(val kind: CheckKind, val label: String, val checked: Boolean)

/** "콩이 · 소형 · 9살 · 멀미 성향 높음". */
data class PetSummaryUi(val name: String, val line: String, val sample: Boolean = false)

data class PlanStopUi(val name: String, val facilities: String, val eta: String)

data class DepartUi(
    val meta: String?,
    val pet: PetSummaryUi,
    val checks: List<CheckUi>,
    /** 체크 3개를 모두 해야 "이대로 출발" (F6-01). */
    val ready: Boolean,
    val plan: List<PlanStopUi>,
    val rationale: String,
    /** "콩이는 약 1시간 10분이 한계예요" (이동 기록이 있을 때). */
    val limitLine: String? = null,
)

// ───────────── 쉼표 (정차) ─────────────

enum class ActivityKind { PET_WALK, PET_CARE, STRETCH }

data class ActivityUi(
    val kind: ActivityKind,
    val who: String,
    val title: String,
    val description: String,
    val recommended: Boolean,
)

data class RestUi(
    val meta: String?,
    val lead: String,
    val minutes: Int,
    /** 충전이 아닌 정차: 사용자가 10/15/20분 고름. */
    val restChoice: Int?,
    val activities: List<ActivityUi>,
    val motion: MotionUi?,
    /** "잠깐 다녀올게" 제한 시간 (기본 10분). */
    val waitLimitMin: Int,
    val battery: String? = null,
)

// ───────────── 잠깐 대기 (F3, 차 화면 = 바깥 사람용 안내) ─────────────

data class ShareUi(
    /** 보호자 폰에서 열 주소. 없으면 "폰 공유 연결 안 됨". */
    val url: String?,
    val connected: Boolean,
)

data class WaitUi(
    /** "주인이 곧 돌아와요". */
    val headline: String,
    /** "3분째". */
    val elapsed: String,
    /** "실내 24℃" 또는 "실내 온도 정보 없음" (지어내지 않는다, F3-06). */
    val cabin: String,
    val battery: String?,
    val limitMin: Int,
    val overdue: Boolean,
    val share: ShareUi,
)

// ───────────── 함께 가는 장소·동물병원 (F4·F5, 정차 중에만) ─────────────

data class PlaceUi(
    val id: String,
    val name: String,
    val category: String,
    /** "경로에서 1.2km · 182km 지점" 또는 "목적지에서 1.4km". */
    val distance: String,
    val policy: String?,
    val sample: Boolean,
)

data class PlacesUi(val title: String, val items: List<PlaceUi>, val note: String?, val loading: Boolean = false)

data class VetUi(val id: String, val name: String, val distance: String, val address: String?, val phone: String?, val sample: Boolean)

data class VetsUi(val items: List<VetUi>, val note: String?, val loading: Boolean = false)

// ───────────── 도착 체크·이동 기록 (F6·F7) ─────────────

data class ArrivalUi(
    /** "콩이 내렸나요?". */
    val question: String,
    /** 덥거나 여름이면 강한 알림 (F6-03). */
    val strong: Boolean,
    val detail: String?,
)

data class TripLogUi(
    val meta: String?,
    val summary: String,
    val durationMin: Int,
    val stops: Int,
    val motionPeak: Int,
    /** 멀미 지수 흐름(0~100, 5분 간격). */
    val motionSeries: List<Int>,
    val limitLine: String,
    /** 동물병원에 보여 줄 요약 줄 (F7-03). */
    val vetSummary: List<String>,
    /** 증상 고르기(키보드 없이). */
    val symptoms: List<SymptomUi>,
)

data class SymptomUi(val label: String, val selected: Boolean)

// ───────────── 반려견 프로필·설정 ─────────────

enum class SizeUi { SMALL, MEDIUM, LARGE }

enum class TendencyUi { LOW, NORMAL, HIGH }

data class PetEditUi(
    val name: String,
    val size: SizeUi,
    val ageYears: Int,
    val tendency: TendencyUi,
    val pottyIntervalMin: Int,
    val petZonePreferred: Boolean,
    /** 저장 전 검사 결과 (비면 저장 가능). */
    val problems: List<String>,
    /** 처음 실행: 닫기 대신 "나중에". */
    val firstRun: Boolean,
)

data class SignalRowUi(val name: String, val inUse: Boolean, val source: String? = null)

data class SettingsUi(
    val timing: Int = 1,
    val voiceSuggest: Boolean = true,
    val reduceMotion: Boolean = false,
    val signals: List<SignalRowUi> = emptyList(),
    val version: String = "",
)
