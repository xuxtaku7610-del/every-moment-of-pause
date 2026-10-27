package com.emop.pause.core.model

/**
 * CarUxRestrictions에서만 나오는 상태. 어디까지 허용할지를 정한다 (architecture §5.1).
 */
enum class DriveState { PARKED, DRIVING, RESTRICTED;

    /** DRIVING·RESTRICTED면 AppMode와 상관없이 즉시 주행 UI (최우선 규칙). */
    val forcesDrivingUi: Boolean get() = this != PARKED
}

/**
 * 화면을 고르는 상태. 무엇을 보여줄지를 정한다 (architecture §5.2).
 */
enum class AppMode { PREPARE, DRIVING, PAUSE, ARRIVED }
