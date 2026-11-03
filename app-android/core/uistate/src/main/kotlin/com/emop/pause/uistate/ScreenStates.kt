package com.emop.pause.uistate

/**
 * 화면 계약: 화면이 그리는 값만 담은 상태 (docs/ui_contract.md).
 * 엔진 상태 → 이 상태 변환은 앱의 `presentation.UiMapper`가 하고, 미리보기·발표 모드 장면은 [Fixtures]가 만든다.
 * 화면은 엔진을 모르고, 사용자 동작은 [UiEvent]로만 돌려보낸다. 디자인(v5)이 오면 화면(Compose)만 갈아 끼운다.
 */

/**
 * 주행 자극 단계 (화면 색·아이콘): 낮음 · 보통(40) · 높음(70). 엔진 값은 **차량 흔들림 누적량**(ISO 2631-1 단순화)이지
 * 반려견 몸을 잰 값이 아니다 (2026-10-31 재정비) — 반려견 상태는 [ObservationUi](보호자 음성 신고)로만 말하고 둘을 섞지 않는다.
 * 주행 화면에는 숫자를 보이지 않는다(정차 중 "주행 자극 보기"·도착 리포트·발표 비교에서만).
 */
enum class MotionTone { CALM, CAUTION, HIGH }

fun motionTone(index: Int): MotionTone = when {
    index >= 70 -> MotionTone.HIGH
    index >= 40 -> MotionTone.CAUTION
    else -> MotionTone.CALM
}

fun motionLabel(t: MotionTone) = when (t) {
    MotionTone.CALM -> "낮음"
    MotionTone.CAUTION -> "보통"
    MotionTone.HIGH -> "높음"
}

/** 주행 자극 "주행 자극 보통 45" (정차 중·리포트·발표 비교에서만). */
data class MotionUi(val index: Int, val tone: MotionTone, val label: String)

/**
 * 콩이 관찰 상태 (재정비 원칙 1): 보호자가 음성으로 신고한 증상만으로 정한다. 신고가 없으면 [line] = "관찰 기록 없음"
 * ("양호"라고 쓰지 않는다). 주행 자극과 섞지 않고, 진단·확률 표현을 쓰지 않는다.
 */
data class ObservationUi(
    /** "관찰 기록 없음" / "신고 2건 · 마지막 헐떡임(출발 39분 뒤)". */
    val line: String,
    val count: Int = 0,
)

/** 다음 길 안내 아이콘 (NaviHelper TBT 종류를 줄인 것). */
enum class GuideIconUi { LEFT, RIGHT, KEEP_LEFT, KEEP_RIGHT, UTURN, STRAIGHT, ROUNDABOUT, TOLL, REST_AREA, ARRIVE, OTHER }

/** 다음 길 안내 카드: "80m · 기업지원허브 방면". [demo]면 시뮬레이터에서 만든 안내(화면에 "시연" 표시). */
data class GuideUi(val icon: GuideIconUi, val distance: String, val towards: String?, val demo: Boolean = false)

// ───────────── 주행 (F1·F2, 주행 중 제한: 텍스트 2줄·버튼 2개·스크롤 없음) ─────────────

sealed interface DriveContent {
    /** 다음 쉼표 안내 한 줄 ("다음 쉼표는 32분 뒤 횡성휴게소예요"). 버튼 없음. */
    data class Calm(val line: String) : DriveContent

    /**
     * 주행 자극 예보 (상태 3): 경고 **한 줄**만 — "3km 뒤 굽은 길 · 12분" + "천천히 가면 덜 흔들려요". 숫자·그래프 없음, 버튼 없음.
     * [level]은 예보 단계(색·아이콘). 근거(경로 형상·속도)는 [basis]로 한 줄 (정차 중 "주행 자극 보기"에서 보인다).
     */
    data class Forecast(
        val title: String,
        val coach: String,
        val level: MotionTone = MotionTone.CAUTION,
        val basis: String? = null,
    ) : DriveContent {
        /** 음성·제한 화면 한 줄. */
        val line: String get() = if (coach.isBlank()) title else "$title · $coach"
    }

    /**
     * 쉼표 추천. [reasons]는 실제 기여 상위 1~2개 ("흔들리는 길을 오래 달렸어요", "반려견 놀이터") — 확인된 시설만.
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

/**
 * 일반 주행 (상태 2, 분할 창 1/3·1/2가 기본): 다음 회전·남은 거리·도착(NaviHelper 실제 값) / 다음 쉼표 / 콩이 관찰 상태 한 줄 / 음성 힌트.
 * 주행 자극 숫자·그래프·도로 그림은 없다(재정비 원칙 2·4). 값이 없으면 그 줄을 숨긴다.
 */
data class DriveUi(
    /** "강릉까지 1시간 10분". */
    val meta: String?,
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
    /** 다음 쉼표 이름·남은 분 (계획이 없으면 null). */
    val nextStopName: String? = null,
    val nextStopMin: Int? = null,
    /** 다음 쉼표의 **확인된** 반려견 시설 ("반려견 놀이터"). 확인 안 된 시설은 넣지 않는다. */
    val nextStopFacilities: List<String> = emptyList(),
    /** 다음 길 안내 (NaviHelper TBT 첫 항목, 시뮬레이터는 시연 안내). 없으면 카드를 숨긴다. */
    val guide: GuideUi? = null,
    /** 목적지까지 "194km · 2시간 33분" (NaviHelper remainDistance·remainTime). */
    val remain: String? = null,
    /** 도착 예정 "오후 7:44". */
    val arrive: String? = null,
    /** 목적지 이름 (기본 내비). 회전 안내가 없을 때 길 안내 카드를 "강릉 · 184km · 2시간 3분 · 도착 오전 11:33" 한 줄로 줄인다. */
    val destination: String? = null,
    /** 콩이 관찰 상태 한 줄 (음성 신고만). */
    val observation: ObservationUi = ObservationUi("관찰 기록 없음"),
    /** 음성 힌트 한 줄 ("“콩이 토했어”라고 말하면 기록해요"). */
    val voiceHint: String? = null,
)

enum class VoiceSheetState { LISTENING, FAILED }

// ───────────── 출발 체크 (F6) ─────────────

enum class CheckKind { HARNESS, WATER, POTTY_BAGS }

data class CheckUi(val kind: CheckKind, val label: String, val checked: Boolean)

/** "콩이 · 소형 · 9살 · 흔들림에 예민한 편(보호자 응답)". */
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
    /** "지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요" (이동 기록이 있을 때). */
    val limitLine: String? = null,
    /** "서울 → 강릉" (경로가 있을 때). */
    val route: String? = null,
    /** "2시간 50분". */
    val duration: String? = null,
    /** 차량 신호가 없거나 에뮬레이터일 때 시연 주행 안내 띠 (DM-10). 실차에는 false. */
    val demoHint: Boolean = false,
) {
    companion object {
        const val DEMO_HINT = "차량 신호가 없어요. 시연 주행으로 둘러볼까요?"
        const val DEMO_START = "시연 주행 시작"
    }
}

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
    /** "잠깐 다녀올게" 제한 시간 (기본 10분). */
    val waitLimitMin: Int,
    val battery: String? = null,
    /** "콩이 돌보기" 체크리스트 (말했던 증상, 정차 중에만). */
    val care: CareUi? = null,
    /** 정차 중 "콩이가 이상해요" 고르기. */
    val distressChips: List<String> = emptyList(),
    /** 콩이 관찰 상태 한 줄 (음성 신고만). */
    val observation: ObservationUi = ObservationUi("관찰 기록 없음"),
    /** "주행 자극 보기" 버튼 (기록이 있을 때만). */
    val stimulusAvailable: Boolean = false,
)

/**
 * 정차 중 "주행 자극 보기" (재정비 원칙 2): 주행 자극 숫자·그래프를 보는 유일한 정차 화면.
 * 고정 문구 "차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님". 값은 엔진 값만.
 */
data class StimulusUi(
    /** 지금 주행 자극. 기록이 없으면 null. */
    val now: MotionUi?,
    /** 최근 30분 (1분 간격). */
    val recent: List<Int>,
    /** 이번 여행 최고. */
    val peak: Int?,
    /** 다음 굽은 길 예보 한 줄 ("3.4km 뒤 굽은 길 · 12분 — 천천히 가면 덜 흔들려요"), 없으면 null. */
    val forecast: String? = null,
    /** 예보 근거 한 줄 ("경로 형상: 시연 경로 (OSM GPX) · 속도 기준"). */
    val basis: String? = null,
    val note: String = STIMULUS_NOTE,
)

const val STIMULUS_NOTE = "차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님"

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
    /** 큰 숫자 "24℃" (11/03 한눈에 크게) — 실내 온도 측정값이 있을 때만. 없으면 줄을 숨긴다. */
    val temp: String? = null,
    /** 공조 에어컨 신호가 실제로 켜짐일 때만 true ("에어컨 켜짐" 작은 표시). 앱은 공조를 켜거나 제어하지 않는다. */
    val acOn: Boolean = false,
    /** 외국인 행인용 영어 한 줄. */
    val english: String = WAIT_ENGLISH,
    /** 제한 시간을 넘김: 아래 보호자 줄에만 앰버 "설정한 10분이 지났어요" (큰 글은 그대로, 빨강·깜박임 없음). */
    val overdueLine: String? = null,
)

const val WAIT_ENGLISH = "My owner will be back soon"
const val WAIT_ENGLISH_RETURNING = "My owner is on the way back"

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
    /** 사전 접수 제휴 병원 (시연용 가상 병원만). 아니면 전화·도착 예정 링크만. */
    val partner: Boolean = false,
) {
    /** "진료 가능 여부는 전화로 확인해요" — 전화번호가 있을 때만 (11/03, 번호 없으면 숨김). */
    val callNote: String? get() = CALL_NOTE.takeIf { phone != null }

    /** 긴급 도움 화면 [사전 접수] 버튼: 제휴(시연용 가상) 병원만, 기존 사전 접수 흐름으로. */
    val intakeButton: Boolean get() = partner

    companion object {
        const val CALL_NOTE = "진료 가능 여부는 전화로 확인해요"
    }
}

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
    /** 오늘 이동 한 줄 요약 (공유에 함께 붙는다): "주행 2시간 10분(휴식 제외) · 주행 자극 최고 46 · 관찰 신고 헐떡임 1회". */
    val tripLine: String? = null,
    /** 병원 사전 접수 (UPGRADE 3): 만들었으면 접수함 링크·상태. */
    val intake: IntakeUi? = null,
    /** 보내기 전에 보여 줄 내용 (전달 동의, 상태 7): 반려견·관찰 신고·메모·오늘 요약·도착 예정. */
    val preview: List<VetRowUi> = emptyList(),
)

/** 긴급 도움 상황 하나 (타일). [id] = 응급 카드 id (breathing·seizure·bleeding·toxin). */
data class EmergencyOptionUi(val id: String, val label: String)

/**
 * 긴급 도움 (2026-11-03, 정차 화면 위 겹침 — 주행 중에는 열리지 않는다). 위 "어떤 상황인가요?" + 상황 4개 + "바로 동물병원 찾기".
 * 고르면 [card](응급 카드) + [vet](가장 가까운 병원, 실제 값만 — 없으면 null) + [경로 안내][전화].
 */
data class EmergencyUi(
    val options: List<EmergencyOptionUi>,
    val selected: String? = null,
    val card: EmergencyCardUi? = null,
    val vet: VetUi? = null,
    val vetLoading: Boolean = false,
    /** 상단 고정 안내. */
    val disclaimer: String = DISCLAIMER,
) {
    companion object {
        const val DISCLAIMER = "응급 판단이나 진단을 대신하지 않아요."
        val OPTIONS = listOf(
            EmergencyOptionUi("breathing", "호흡 곤란"),
            EmergencyOptionUi("seizure", "경련·의식 이상"),
            EmergencyOptionUi("bleeding", "외상·출혈"),
            EmergencyOptionUi("toxin", "위험 물질 섭취"),
        )
    }
}

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

/**
 * 도착 후 기록 리포트 (상태 8, 기존 이동 기록 대체). 주인공은 **시간순 타임라인** — 출발 → 주행 자극 높은 구간 → 관찰 신고 → 휴식 →
 * 병원 이동 → 도착. 데이터가 없는 줄은 빼고 지어내지 않는다. 경로선은 그리지 않는다(위치를 남기지 않음, 사용자 결정 10/31 안 A).
 */
data class TripLogUi(
    val meta: String?,
    val petName: String = "",
    /** 요약 한 줄 수치: 총 이동 시간(분, 정차 포함) · 거리("194km", 모르면 null) · 휴식 횟수 · 관찰 신고 횟수. */
    val totalMin: Int,
    val distance: String?,
    val stops: Int,
    val reports: Int,
    /** 시간순 타임라인. */
    val timeline: List<TimelineUi>,
    /** 주행 자극 흐름(0~100, 5분 간격, 각 점 = 그 5분 최고). 기록이 없으면 빈 목록. */
    val stimulusSeries: List<Int>,
    /** 그래프 표시: 타임라인 항목 위치(휴식·신고·높은 구간). */
    val marks: List<TrendMarkUi> = emptyList(),
    /** 이번 여행 한 줄 ("이번엔 출발 2시간 14분 뒤 주행 자극이 보통 이상이 됐어요."). */
    val insight: String? = null,
    /** 기록 요약 문장 (LLM·템플릿, F7-04). */
    val summary: String = "",
    /** "병원에 공유" — 관찰 신고·시각·주행 요약 (값이 있는 줄만). */
    val share: List<VetRowUi> = emptyList(),
    /** 이전 기록 (최근 순, 최대 3). */
    val history: List<HistoryUi> = emptyList(),
    /** 개인 보정 한 줄 ("콩이 맞춤 정도: 기록 4회, 민감도 보통 → 높음") — 신고에서 자동으로 쌓인다. */
    val calibration: String? = null,
)

enum class TimelineKind { START, STIMULUS, REPORT, REST, VET, ARRIVE }

/** 타임라인 한 줄: [time]은 출발 뒤 "2:14", [minute]은 출발 뒤 분(그래프 위치). */
data class TimelineUi(val kind: TimelineKind, val time: String, val title: String, val detail: String? = null, val minute: Int = 0)

enum class TrendMarkKind { STOP, SYMPTOM, HIGH }

/** 그래프 표시 하나. [minute]은 출발 뒤 분(정차 포함), [lengthMin]은 휴식·높은 구간 길이. */
data class TrendMarkUi(val kind: TrendMarkKind, val minute: Int, val label: String, val lengthMin: Int = 0)

data class VetRowUi(val key: String, val value: String)

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

// ───────────── 온보딩 3단계 (11번): 반려견 프로필 → 차량 연동 → 흔들림에 예민한 정도(보호자 응답) ─────────────

data class QuizUi(val question: String, val options: List<String>, val selected: Int?)

data class OnboardingUi(
    /** 1 프로필 · 2 차량 연동 · 3 흔들림에 예민한 정도(보호자 응답). */
    val step: Int,
    val pet: PetEditUi,
    /** 2단계: 이 차에서 확인한 신호. */
    val signals: List<SignalRowUi>,
    /** 3단계: 질문 3개. */
    val quiz: List<QuizUi>,
    /** 3개 다 답하면 결과(흔들림에 예민한 정도 — 보호자 응답이지 측정·진단이 아니다). */
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
