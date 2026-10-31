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

    /**
     * 멀미 예보 ("3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요"). 버튼 없음.
     * v5 화면은 [title] 1줄 + [coach] 1줄 + 정지된 그래프([now] → [peak], 20분)로 그린다. 둘이 없으면 [line].
     */
    data class Forecast(
        val line: String,
        val title: String? = null,
        val coach: String? = null,
        val now: Int? = null,
        val peak: Int? = null,
        /** v6 그래프 (엔진 값): 굽은 길 시작까지 분·굽은 길 지속 분·시작까지 m. 모르면 null → 그래프 없이 수치만. */
        val startMin: Int? = null,
        val durationMin: Int? = null,
        val startM: Int? = null,
        /** 엔진 예측 곡선: 지금부터 1분 간격 예상 지수 (0번 = 지금, 최대 21개). 그래프는 이 점만 잇는다. */
        val series: List<Int> = emptyList(),
    ) : DriveContent

    /**
     * 쉼표 추천. [reasons]는 실제 기여 상위 1~2개 ("콩이 멀미가 쌓였어요", "반려견 놀이터").
     * 버튼 2개(여기서 쉴게요·나중에) + 음성 힌트. [urgent](운전자 피로 위험)면 거절 버튼 없이 1개.
     */
    data class Suggest(
        val minutes: Int,
        val place: String,
        val reasons: List<String>,
        val urgent: Boolean = false,
        /** "8km" (경로 거리, 모르면 null). */
        val distance: String? = null,
        /** "강릉 방향". */
        val direction: String? = null,
    ) : DriveContent

    /** 수락해서 가고 있는 쉼표. 버튼 없음. */
    data class Heading(val minutes: Int, val place: String) : DriveContent

    /** 동물병원 가는 길 (주행 중엔 1순위 한 곳 2줄만, 목록·가이드·메모는 정차 중). 버튼 없음. */
    /** [clinic]: 병원 사전 접수 상태 한 줄(24자 이하, UPGRADE 3) — "병원: 도착하면 바로 봐요". */
    data class Vet(val name: String, val distance: String, val minutes: Int?, val clinic: String? = null) : DriveContent

    /**
     * "콩이가 이상해요" (UPGRADE 7): 안내 2줄 이하([line]) + 버튼 2개(정차 장소·병원으로 / 괜찮아졌어요) + 고정 하단 문구.
     * [target]은 버튼에 싣는 장소 이름(14자), 없으면 "알겠어요".
     */
    data class Distress(val line: String, val target: String?, val minutes: Int?, val vet: Boolean = false) : DriveContent
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
    /** 상태 패널 이름 ("콩이"). */
    val pet: String = "",
    /** "마지막 휴식 후 52분" (모르면 null). */
    val sinceRest: String? = null,
    /** "약 72km" — 다음 쉼표까지 (계획이 없으면 null). */
    val nextStop: String? = null,
    /** v6 상태 패널: 다음 쉼표 이름·남은 분 (계획이 없으면 null). */
    val nextStopName: String? = null,
    val nextStopMin: Int? = null,
    /** 앞 1km 경로 곡률(1/m, 50m 간격, 왼쪽 +). 도로 일러스트가 휘는 정도 — 없으면 곧은 길. */
    val roadAhead: List<Float> = emptyList(),
    /** 다음 굽은 길까지 거리 ("3.2km", 형상이 없으면 null) — 상태 패널 빈자리 대신 (엔진 값). */
    val nextCurve: String? = null,
    /** 최근 30분 멀미 지수 (1분 간격, 엔진 값). 2점 미만이면 그래프 없음. */
    val recentMotion: List<Int> = emptyList(),
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
    /** "서울 → 강릉" (경로가 있을 때). */
    val route: String? = null,
    /** "2시간 50분". */
    val duration: String? = null,
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
    /** "콩이 돌보기" 체크리스트 (말했던 증상, 정차 중에만). */
    val care: CareUi? = null,
    /** 정차 중 "콩이가 이상해요" 고르기. */
    val distressChips: List<String> = emptyList(),
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
    /** "실내 24℃" — 실내 온도 측정값이 있을 때만. 없으면 null → 온도 표시를 숨긴다 (지어내지 않는다, F3-06·UPGRADE 2). */
    val cabin: String?,
    val battery: String?,
    val limitMin: Int,
    val overdue: Boolean,
    val share: ShareUi,
    /** 대기 중 쓴 배터리 "1%" — 신호가 있을 때만. */
    val batteryUsed: String? = null,
    /** 켤 때 한 번: "반려견을 차에 오래 혼자 두지 마세요. 이 화면은 보조 안내예요." */
    val notice: String? = null,
    /** 대기 중 온도가 오를 때 "실내 온도가 오르고 있어요 (24℃ → 27℃)". */
    val heat: String? = null,
    /** 온도를 모를 때 6~9월에만 "여름에는 차 안이 빨리 더워져요". */
    val seasonNote: String? = null,
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
    /** TourAPI 대표 이미지(https). 없으면 회색 자리. */
    val imageUrl: String? = null,
    /** 동반 구분 "전구역 동반"/"일부구역 동반" (API 필드)만. */
    val tags: List<String> = emptyList(),
    /** 동반 조건 원문 문장(가능 동물·실내/실외·필요 사항) — 추측하지 않고 그대로. */
    val notes: List<String> = emptyList(),
    /** 필터용: 글에 명시됐을 때만 true/false, 모르면 null ("정보 없음"). */
    val largeDogOk: Boolean? = null,
    val indoorOk: Boolean? = null,
)

data class PlacesUi(
    val title: String,
    val items: List<PlaceUi>,
    val note: String?,
    val loading: Boolean = false,
    /** 사진이 하나라도 있으면 "사진 한국관광공사". */
    val credit: String? = null,
)

data class VetUi(
    val id: String,
    val name: String,
    val distance: String,
    val address: String?,
    val phone: String?,
    val sample: Boolean,
    /** 예상 분 (평균 속도 가정). */
    val minutes: Int? = null,
    /** 인허가 상태 "영업 중"(실데이터만). 영업시간·24시는 데이터에 없어 쓰지 않는다. */
    val status: String? = null,
)

data class VetsUi(
    val items: List<VetUi>,
    val note: String?,
    val loading: Boolean = false,
    /** 증상 메모 버튼(구토·설사·헐떡임·축 처짐·떨림·기타). */
    val memo: List<SymptomUi> = emptyList(),
    /** 도착 예정 공유 링크 (만들었으면). */
    val shareUrl: String? = null,
    /** 받아쓴 메모(화면에 그대로, 고칠 수 있음). 기기에만 저장. */
    val memoText: String = "",
    /** 공유할 때 증상 메모·오늘 요약을 넣을지 (체크박스). */
    val memoInclude: Boolean = false,
    /** 음성 메모를 듣는 중. */
    val memoListening: Boolean = false,
    /** 오늘 이동 한 줄 요약 (공유에 함께 붙는다): "이동 2시간 10분 · 멀미 주의 1회(최고 46)". */
    val tripLine: String? = null,
    /** 병원 사전 접수 (UPGRADE 3): 만들었으면 접수함 링크·상태. */
    val intake: IntakeUi? = null,
)

/**
 * 병원 사전 접수 (UPGRADE 3, F5-07~10). [clinicUrl]은 병원 접수함(보호자가 직접 전달), [status]는 한 줄 상태
 * ("병원: 도착하면 바로 볼게요 · 9분 뒤 도착"). [busy]면 병원이 어렵다고 답한 것 → 다른 병원 고르기를 앞에.
 */
data class IntakeUi(
    val vetName: String,
    val clinicUrl: String,
    val status: String,
    val etaMinutes: Int,
    val replied: Boolean = false,
    val busy: Boolean = false,
    val arrived: Boolean = false,
)

// ───────────── 도착 체크·이동 기록 (F6·F7) ─────────────

data class ArrivalUi(
    /** "콩이 내렸나요?". */
    val question: String,
    /** 덥거나 여름이면 강한 알림 (F6-03). */
    val strong: Boolean,
    val detail: String?,
    /** 경고 카드 제목: 온도가 있으면 단계별("실내 31℃ — 매우 더워요!"), 없으면 여름에만 일반 문구. 실내 온도는 신호가 있을 때만 [cabin]. */
    val warning: String? = null,
    val cabin: String? = null,
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
    /** 멀미 주의(40↑)에 들어간 횟수. */
    val warnings: Int = 0,
    /** 이전 기록 (최근 순, 최대 3). */
    val history: List<HistoryUi> = emptyList(),
    /** 도착 뒤 한 번만: "오늘 콩이 멀미했어요?" (답하거나 건너뛰면 null). */
    val askMotion: String? = null,
    /** 오늘 증상 기록 한 줄: "오늘 헐떡임 1회(출발 10분 뒤)". */
    val events: String? = null,
    /** 개인 보정 한 줄 (UPGRADE 4): 답한 뒤 "콩이 맞춤 정도: 기록 4회, 민감도 보통 → 조금 높음". */
    val calibration: String? = null,
    /** 상태 카드 제목 "오늘 콩이 상태". */
    val petName: String = "",
    /** 오늘 상태 (v7, 판정 규칙 UiMapper.petStatus). 기록이 없으면 확인 중. */
    val status: PetStatusUi = PetStatusUi.CHECKING,
    /** 상태 근거 한 줄: "헐떡임 1번 · 멀미 주의 1번". */
    val statusReason: String? = null,
    /** 도착 시점 멀미 지수 ("내렸어요" 때 엔진 값). 기록이 없으면 null. */
    val arrivalIndex: Int? = null,
    /** 그래프 표시: 휴식 구간·주행 중 증상 기록 (출발 뒤 분, 그래프 가로축과 같은 시계). */
    val marks: List<TrendMarkUi> = emptyList(),
    /** 병원 공유 요약 표 (값이 없는 줄은 뺀다). */
    val vetRows: List<VetRowUi> = emptyList(),
)

/** 이동 기록 상태 4단계. 색은 상태마다 하나(초록·앰버·빨강·회색)이고 항상 아이콘·글자와 함께 쓴다. */
enum class PetStatusUi { CHECKING, GOOD, CAUTION, ATTENTION }

enum class TrendMarkKind { STOP, SYMPTOM }

/** 그래프 표시 하나. [minute]은 출발 뒤 분(정차 포함), [lengthMin]은 휴식 구간 길이. */
data class TrendMarkUi(val kind: TrendMarkKind, val minute: Int, val label: String, val lengthMin: Int = 0)

data class VetRowUi(val key: String, val value: String)

/** "오늘 멀미했어요?" 답. 건너뛰기는 기록하지 않는다. */
enum class MotionAnswerUi { YES, LITTLE, NO }

data class HistoryUi(val date: String, val line: String)

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
    /** "콩이 맞춤 정도: 기록 5회, 민감도 보통 → 조금 높음" (기록이 있을 때). */
    val calibration: String? = null,
)

data class SignalRowUi(val name: String, val inUse: Boolean, val source: String? = null)

// ───────────── 온보딩 3단계 (11번): 반려견 프로필 → 차량 연동 → 멀미 민감도 진단 ─────────────

data class QuizUi(val question: String, val options: List<String>, val selected: Int?)

data class OnboardingUi(
    /** 1 프로필 · 2 차량 연동 · 3 멀미 민감도. */
    val step: Int,
    val pet: PetEditUi,
    /** 2단계: 이 차에서 확인한 신호. */
    val signals: List<SignalRowUi>,
    /** 3단계: 질문 3개. */
    val quiz: List<QuizUi>,
    /** 3개 다 답하면 결과(멀미 성향). */
    val result: TendencyUi?,
) {
    val canNext: Boolean get() = when (step) {
        1 -> pet.problems.isEmpty()
        3 -> result != null
        else -> true
    }
}

data class SettingsUi(
    val timing: Int = 1,
    val voiceSuggest: Boolean = true,
    val reduceMotion: Boolean = false,
    val signals: List<SignalRowUi> = emptyList(),
    val version: String = "",
    /** "앞 구간 형상: 시연 경로 (OSM GPX)" / "회전 안내 + 진행 방위" (UPGRADE 5). */
    val shapeSource: String? = null,
    /** 앱 정보 "데이터 출처" — 라이선스상 꼭 보여야 하는 표기(OSM ODbL 등). 디자인이 바뀌어도 빼지 않는다. */
    val credits: List<String> = DATA_CREDITS,
)

val DATA_CREDITS = listOf(
    "고갯길 시연 경로(대관령 옛길): © OpenStreetMap contributors, ODbL 1.0 — openstreetmap.org/copyright",
    "반려동물 동반 여행지·장소 사진: 한국관광공사 TourAPI 반려동물 동반여행 (서버에 데이터가 없으면 샘플)",
    "동물병원: 지방행정 인허가 데이터(동물병원) — 영업 상태만 있고 영업시간은 없음 (파일이 없으면 샘플)",
    "아이콘: Material Symbols Rounded (Apache-2.0), Microsoft Fluent UI Emoji Flat (MIT)",
    "글꼴: Pretendard (SIL OFL 1.1)",
)
