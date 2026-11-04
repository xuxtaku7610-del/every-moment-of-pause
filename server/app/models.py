"""SQLAlchemy 모델. database.md §2 DDL과 같다.

단위 테스트는 SQLite in-memory에서 돌므로 JSONB·BIGSERIAL은 방언별 타입으로 둔다.
"""
from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    JSON,
    BigInteger,
    Boolean,
    CheckConstraint,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    LargeBinary,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

JsonType = JSON().with_variant(JSONB(), "postgresql")
BigIdType = BigInteger().with_variant(Integer, "sqlite")
Tz = DateTime(timezone=True)

EVENT_TYPES = (
    "recommendation_shown", "recommendation_accepted", "recommendation_declined",
    "stop_started", "stop_ended", "fatigue_peak", "motion_peak",
    "pause_activity_started", "pause_activity_ended", "wait_started", "wait_ended",
)


def _in(column: str, values: tuple[str, ...]) -> str:
    return f"{column} IN ({', '.join(repr(v) for v in values)})"


class Base(DeclarativeBase):
    pass


class Device(Base):
    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    token_hash: Mapped[str] = mapped_column(String(128))
    platform: Mapped[str] = mapped_column(String(32))
    app_version: Mapped[str] = mapped_column(String(16))
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    last_seen_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class RestStop(Base):
    __tablename__ = "rest_stops"
    __table_args__ = (
        CheckConstraint(_in("stop_type", ("service_area", "drowsy_shelter")), name="ck_rest_stops_stop_type"),
        Index("idx_rest_stops_route", "route_name", "direction"),
        Index("idx_rest_stops_latlng", "lat", "lng"),
    )

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    ex_code: Mapped[str] = mapped_column(String(32), unique=True)
    name: Mapped[str] = mapped_column(String(64))
    stop_type: Mapped[str] = mapped_column(String(16))
    route_name: Mapped[str] = mapped_column(String(64))
    direction: Mapped[str] = mapped_column(String(32))
    lat: Mapped[float] = mapped_column(Float(precision=53))
    lng: Mapped[float] = mapped_column(Float(precision=53))
    phone: Mapped[str | None] = mapped_column(String(32))
    address: Mapped[str | None] = mapped_column(String(255))
    updated_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class FacilityCode(Base):
    __tablename__ = "facility_codes"

    code: Mapped[str] = mapped_column(String(32), primary_key=True)
    label_ko: Mapped[str] = mapped_column(String(64))


class RestStopFacility(Base):
    __tablename__ = "rest_stop_facilities"
    __table_args__ = (CheckConstraint("quantity >= 0", name="ck_rest_stop_facilities_quantity"),)

    rest_stop_id: Mapped[str] = mapped_column(
        String(32), ForeignKey("rest_stops.id", ondelete="CASCADE"), primary_key=True
    )
    facility_code: Mapped[str] = mapped_column(String(32), ForeignKey("facility_codes.code"), primary_key=True)
    quantity: Mapped[int] = mapped_column(Integer, server_default=text("1"))


class Trip(Base):
    __tablename__ = "trips"
    __table_args__ = (
        CheckConstraint(_in("status", ("active", "ended")), name="ck_trips_status"),
        CheckConstraint(_in("powertrain", ("ev", "ice", "hev")), name="ck_trips_powertrain"),
        Index("idx_trips_device", "device_id", text("started_at DESC")),
    )

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    status: Mapped[str] = mapped_column(String(16), server_default=text("'active'"))
    origin_label: Mapped[str | None] = mapped_column(String(64))
    destination_label: Mapped[str | None] = mapped_column(String(64))
    planned_distance_km: Mapped[Decimal | None] = mapped_column(Numeric(7, 1))
    actual_distance_km: Mapped[Decimal | None] = mapped_column(Numeric(7, 1))
    powertrain: Mapped[str | None] = mapped_column(String(8))
    passenger_summary: Mapped[dict] = mapped_column(JsonType, server_default=text("'{}'"))
    started_at: Mapped[datetime] = mapped_column(Tz)
    ended_at: Mapped[datetime | None] = mapped_column(Tz)


class TripEvent(Base):
    __tablename__ = "trip_events"
    __table_args__ = (
        CheckConstraint(_in("event_type", EVENT_TYPES), name="ck_trip_events_event_type"),
        UniqueConstraint("trip_id", "client_event_id", name="uq_trip_events_client"),
        Index("idx_trip_events_trip", "trip_id", "occurred_at"),
    )

    id: Mapped[int] = mapped_column(BigIdType, primary_key=True, autoincrement=True)
    trip_id: Mapped[str] = mapped_column(String(32), ForeignKey("trips.id", ondelete="CASCADE"))
    client_event_id: Mapped[str] = mapped_column(String(64))
    event_type: Mapped[str] = mapped_column(String(32))
    rest_stop_id: Mapped[str | None] = mapped_column(String(32), ForeignKey("rest_stops.id"))
    payload: Mapped[dict] = mapped_column(JsonType, server_default=text("'{}'"))
    occurred_at: Mapped[datetime] = mapped_column(Tz)
    received_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class LlmCallLog(Base):
    __tablename__ = "llm_call_logs"
    __table_args__ = (CheckConstraint(_in("source", ("ollama", "template")), name="ck_llm_call_logs_source"),)

    id: Mapped[int] = mapped_column(BigIdType, primary_key=True, autoincrement=True)
    device_id: Mapped[str | None] = mapped_column(String(32), ForeignKey("devices.id", ondelete="SET NULL"))
    kind: Mapped[str] = mapped_column(String(32))
    source: Mapped[str] = mapped_column(String(16))
    latency_ms: Mapped[int] = mapped_column(Integer)
    fallback_used: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


# shop = TourAPI 쇼핑(콘텐츠 타입 38, 반려동물 용품점·마켓) — 주행 중 주변 추천 "마켓" (UI v9, 마이그레이션 0006)
PLACE_CATEGORIES = ("restaurant", "cafe", "lodging", "tour", "walk", "shop")


class PetPlace(Base):
    """반려동물 동반 장소 (F4, 마이그레이션 0003). source: tourapi | sample."""
    __tablename__ = "pet_places"
    __table_args__ = (
        CheckConstraint(_in("source", ("tourapi", "sample")), name="ck_pet_places_source"),
        CheckConstraint(_in("category", PLACE_CATEGORIES), name="ck_pet_places_category"),
        Index("idx_pet_places_latlng", "lat", "lng"),
    )

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    source_id: Mapped[str] = mapped_column(String(64), unique=True)
    source: Mapped[str] = mapped_column(String(16))
    name: Mapped[str] = mapped_column(String(128))
    category: Mapped[str] = mapped_column(String(16))
    lat: Mapped[float] = mapped_column(Float)
    lng: Mapped[float] = mapped_column(Float)
    address: Mapped[str | None] = mapped_column(String(256))
    pet_policy: Mapped[str | None] = mapped_column(Text)
    # 0004: TourAPI detailPetTour2·대표 이미지. 모르면 NULL(지어내지 않는다).
    image_url: Mapped[str | None] = mapped_column(String(512))
    space_note: Mapped[str | None] = mapped_column(String(100))
    size_note: Mapped[str | None] = mapped_column(String(100))
    # 필터 (UPGRADE 6): 명시됐을 때만 True/False, 모르면 NULL("정보 없음")
    large_dog_ok: Mapped[bool | None] = mapped_column(Boolean)
    indoor_ok: Mapped[bool | None] = mapped_column(Boolean)
    companion: Mapped[str | None] = mapped_column(String(64))
    updated_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class VetClinic(Base):
    """동물병원 (F5, 마이그레이션 0003). source: licensing | sample. 샘플은 전화번호를 지어내지 않는다."""
    __tablename__ = "vet_clinics"
    __table_args__ = (
        CheckConstraint(_in("source", ("licensing", "sample")), name="ck_vet_clinics_source"),
        Index("idx_vet_clinics_latlng", "lat", "lng"),
    )

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    license_no: Mapped[str] = mapped_column(String(64), unique=True)
    source: Mapped[str] = mapped_column(String(16))
    name: Mapped[str] = mapped_column(String(128))
    address: Mapped[str | None] = mapped_column(String(256))
    phone: Mapped[str | None] = mapped_column(String(32))
    lat: Mapped[float] = mapped_column(Float)
    lng: Mapped[float] = mapped_column(Float)
    open: Mapped[bool] = mapped_column(Boolean, server_default=text("true"))
    updated_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


WAIT_STATES = ("waiting", "overdue", "ended")


class WaitSession(Base):
    """대기 공유 (F3, 마이그레이션 0003). 토큰 원문 대신 SHA-256 해시가 키. **위치 컬럼을 두지 않는다.**"""
    __tablename__ = "wait_sessions"
    __table_args__ = (
        CheckConstraint(_in("state", WAIT_STATES), name="ck_wait_sessions_state"),
        CheckConstraint("limit_min BETWEEN 1 AND 120", name="ck_wait_sessions_limit"),
        Index("idx_wait_sessions_device", "device_id"),
    )

    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    state: Mapped[str] = mapped_column(String(16), server_default=text("'waiting'"))
    limit_min: Mapped[int] = mapped_column(Integer)
    elapsed_sec: Mapped[int] = mapped_column(Integer, server_default=text("0"))
    battery_percent: Mapped[int | None] = mapped_column(Integer)
    cabin_temp_c: Mapped[float | None] = mapped_column(Float)
    # 0004: 대기 중 처음 받은 실내 온도 — 오르는지 비교 (UPGRADE 2)
    cabin_temp_start_c: Mapped[float | None] = mapped_column(Float)
    # 0004: 보호자가 폰 페이지에서 "돌아가는 중"을 눌렀다 (차 화면 문구가 바뀐다).
    guardian_returning: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))
    # 0007 (v10-3): 누가 "돌아가는 중"을 눌렀는지 — 이름 대신 보호자 1·2 (같은 링크를 두 사람이 연다)
    returning_by: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(Tz)


class EtaShare(Base):
    """동물병원 도착 예정 공유 (F5, 마이그레이션 0004). 위치 없이 이름·병원·도착 예정 시각·증상 메모만. 2시간 만료."""
    __tablename__ = "eta_shares"
    __table_args__ = (
        CheckConstraint("eta_minutes BETWEEN 0 AND 600", name="ck_eta_shares_eta"),
        CheckConstraint("clinic_reply IS NULL OR clinic_reply IN ('received', 'ready', 'busy')", name="ck_eta_shares_reply"),
        Index("idx_eta_shares_device", "device_id"),
    )

    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    pet_name: Mapped[str] = mapped_column(String(32))
    vet_name: Mapped[str] = mapped_column(String(128))
    eta_minutes: Mapped[int] = mapped_column(Integer)
    symptoms: Mapped[str | None] = mapped_column(String(200))
    memo: Mapped[str | None] = mapped_column(String(200))
    trip_line: Mapped[str | None] = mapped_column(String(80))
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(Tz)
    # 동물병원 사전 접수 (UPGRADE 3, 마이그레이션 0005): 병원 접수함 링크는 보호자 링크와 다른 토큰(해시만)
    clinic_token_hash: Mapped[str | None] = mapped_column(String(64), unique=True)
    eta_at: Mapped[datetime | None] = mapped_column(Tz)
    updated_at: Mapped[datetime | None] = mapped_column(Tz)
    arrived: Mapped[bool] = mapped_column(Boolean, default=False, server_default=text("false"))
    clinic_reply: Mapped[str | None] = mapped_column(String(16))
    clinic_reply_at: Mapped[datetime | None] = mapped_column(Tz)
    clinic_seen_at: Mapped[datetime | None] = mapped_column(Tz)


class ReportShare(Base):
    """폰으로 받는 이동 기록 (v10-3, 마이그레이션 0007). 앱이 만든 요약(타임라인·흔들림 값·병원 요약)만 — **위치 없음**. 24시간 만료."""
    __tablename__ = "report_shares"
    __table_args__ = (Index("idx_report_shares_device", "device_id"),)

    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    payload: Mapped[dict] = mapped_column(JsonType)
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(Tz)


class PhotoUpload(Base):
    """반려견 사진 QR 업로드 (v10-3, 마이그레이션 0007). 10분 만료, 이미지 1장(5MB 이하). 차가 받아 가면 이미지 바이트를 지운다."""
    __tablename__ = "photo_uploads"
    __table_args__ = (
        CheckConstraint("content_type IS NULL OR content_type IN ('image/jpeg', 'image/png', 'image/webp')", name="ck_photo_uploads_type"),
        Index("idx_photo_uploads_device", "device_id"),
    )

    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    image: Mapped[bytes | None] = mapped_column(LargeBinary)
    content_type: Mapped[str | None] = mapped_column(String(16))
    uploaded_at: Mapped[datetime | None] = mapped_column(Tz)
    fetched_at: Mapped[datetime | None] = mapped_column(Tz)
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(Tz)
