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
    "stop_started", "stop_ended", "fatigue_peak", "kid_question",
    "pause_activity_started", "pause_activity_ended",
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


class ContentEpisode(Base):
    __tablename__ = "content_episodes"
    __table_args__ = (
        CheckConstraint(_in("topic", ("proverb", "common_sense", "safety", "manners")), name="ck_content_episodes_topic"),
        CheckConstraint("duration_sec BETWEEN 30 AND 900", name="ck_content_episodes_duration"),
        CheckConstraint("min_age <= max_age", name="ck_content_episodes_age"),
        Index("idx_episodes_lookup", "topic", "reviewed", "min_age", "max_age"),
    )

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    topic: Mapped[str] = mapped_column(String(32))
    title: Mapped[str] = mapped_column(String(128))
    script: Mapped[str] = mapped_column(Text)
    quiz: Mapped[dict | None] = mapped_column(JsonType)
    duration_sec: Mapped[int] = mapped_column(Integer)
    min_age: Mapped[int] = mapped_column(Integer, server_default=text("4"))
    max_age: Mapped[int] = mapped_column(Integer, server_default=text("12"))
    reviewed: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class Mission(Base):
    __tablename__ = "missions"
    __table_args__ = (CheckConstraint(_in("context", ("rest_stop", "charging")), name="ck_missions_context"),)

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    context: Mapped[str] = mapped_column(String(16))
    title: Mapped[str] = mapped_column(String(64))
    instructions: Mapped[str] = mapped_column(Text)
    duration_sec: Mapped[int] = mapped_column(Integer)
    min_age: Mapped[int] = mapped_column(Integer, server_default=text("4"))
    reviewed: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))


class Playlist(Base):
    __tablename__ = "playlists"

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    device_id: Mapped[str] = mapped_column(String(32), ForeignKey("devices.id", ondelete="CASCADE"))
    target_sec: Mapped[int] = mapped_column(Integer)
    total_sec: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(Tz, server_default=func.now())


class PlaylistItem(Base):
    __tablename__ = "playlist_items"

    playlist_id: Mapped[str] = mapped_column(
        String(32), ForeignKey("playlists.id", ondelete="CASCADE"), primary_key=True
    )
    position: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=False)
    episode_id: Mapped[str] = mapped_column(String(32), ForeignKey("content_episodes.id"))


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
