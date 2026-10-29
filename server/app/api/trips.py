"""여행 API — api_spec §2.10~2.13. 휴게소 단위 이벤트만 받는다(경로 좌표는 받지 않음, architecture §6)."""
import secrets
from datetime import datetime
from typing import Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, require_device
from app.errors import ApiError
from app.models import EVENT_TYPES, Trip, TripEvent

router = APIRouter()
EventType = Literal[EVENT_TYPES]  # type: ignore[valid-type]


class PassengerSummary(BaseModel):
    """인원 수만 받는다. 동승자 이름·나이는 서버로 보내지 않는다 (개인정보 안내 화면, 결정 2026-10-29).
    알 수 없는 필드(나이 목록 등)가 오면 422로 거절해 실수로라도 저장되지 않게 한다."""
    model_config = {"extra": "forbid"}

    adults: int = Field(default=1, ge=0, le=9)
    children: int = Field(default=0, ge=0, le=9)
    pets: int = Field(default=0, ge=0, le=9)


class TripIn(BaseModel):
    startedAt: datetime
    originLabel: str | None = Field(default=None, max_length=64)
    destinationLabel: str | None = Field(default=None, max_length=64)
    plannedDistanceKm: float | None = Field(default=None, ge=0, le=5000)
    passengerSummary: PassengerSummary = Field(default_factory=PassengerSummary)
    powertrain: Literal["ev", "ice", "hev"] | None = None


class EventIn(BaseModel):
    clientEventId: str = Field(min_length=1, max_length=64)
    type: EventType
    occurredAt: datetime
    restStopId: str | None = Field(default=None, max_length=32)
    payload: dict = Field(default_factory=dict)


class EventsIn(BaseModel):
    events: list[EventIn] = Field(min_length=1, max_length=500)


class TripPatch(BaseModel):
    status: Literal["ended"]
    endedAt: datetime
    actualDistanceKm: float | None = Field(default=None, ge=0, le=5000)


def _own_trip(session: Session, trip_id: str, device_id: str) -> Trip:
    trip = session.get(Trip, trip_id)
    # 다른 기기의 여행은 없는 것과 같다 (존재 여부도 알려 주지 않는다).
    if trip is None or trip.device_id != device_id:
        raise ApiError(404, "TRIP_NOT_FOUND", "해당 여행을 찾을 수 없습니다.", {"tripId": trip_id})
    return trip


@router.post("/trips", status_code=201)
def create(body: TripIn, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    tid = f"tr_{secrets.token_hex(4)}"
    session.add(Trip(
        id=tid, device_id=device_id, origin_label=body.originLabel, destination_label=body.destinationLabel,
        planned_distance_km=body.plannedDistanceKm, powertrain=body.powertrain,
        passenger_summary=body.passengerSummary.model_dump(), started_at=body.startedAt,
    ))
    session.commit()
    return {"tripId": tid, "status": "active"}


@router.post("/trips/{trip_id}/events", status_code=202)
def events(trip_id: str, body: EventsIn, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    trip = _own_trip(session, trip_id, device_id)
    if trip.status == "ended":
        raise ApiError(409, "TRIP_ALREADY_ENDED", "이미 끝난 여행입니다.", {"tripId": trip_id})
    seen = set(session.scalars(select(TripEvent.client_event_id).where(TripEvent.trip_id == trip_id)))
    accepted = duplicates = 0
    for e in body.events:
        if e.clientEventId in seen:
            duplicates += 1
            continue
        seen.add(e.clientEventId)
        session.add(TripEvent(
            trip_id=trip_id, client_event_id=e.clientEventId, event_type=e.type,
            rest_stop_id=e.restStopId, payload=e.payload, occurred_at=e.occurredAt,
        ))
        accepted += 1
    session.commit()
    return {"accepted": accepted, "duplicates": duplicates}


@router.patch("/trips/{trip_id}")
def end(trip_id: str, body: TripPatch, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    trip = _own_trip(session, trip_id, device_id)
    if trip.status == "ended":
        raise ApiError(409, "TRIP_ALREADY_ENDED", "이미 끝난 여행입니다.", {"tripId": trip_id})
    trip.status, trip.ended_at, trip.actual_distance_km = "ended", body.endedAt, body.actualDistanceKm
    session.commit()
    return {"tripId": trip_id, "status": "ended"}


@router.get("/trips/{trip_id}/report")
def report(trip_id: str, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    trip = _own_trip(session, trip_id, device_id)
    evs = list(session.scalars(select(TripEvent).where(TripEvent.trip_id == trip_id).order_by(TripEvent.occurred_at)))
    return build_report(trip, evs)


def build_report(trip: Trip, evs: list[TripEvent]) -> dict:
    """여행 리포트 (api_spec §2.13). 같은 이벤트를 다시 올려도 수치는 같다(중복은 적재되지 않음, test_plan T7-4)."""
    def of(kind: str) -> list[TripEvent]:
        return [e for e in evs if e.event_type == kind]

    def aware(t: datetime) -> datetime:
        # SQLite는 시간대를 지운다. 비교만 하므로 같은 기준(UTC)으로 맞춘다.
        from datetime import timezone
        return t if t.tzinfo else t.replace(tzinfo=timezone.utc)

    start = aware(trip.started_at)
    end = aware(trip.ended_at) if trip.ended_at else (aware(evs[-1].occurred_at) if evs else start)
    # 최장 연속 주행: 출발·정차 끝 → 다음 정차 시작 사이 가장 긴 구간.
    starts = [start] + [aware(e.occurred_at) for e in of("stop_ended")]
    stops_at = [aware(e.occurred_at) for e in of("stop_started")] + [end]
    longest = 0
    for s in starts:
        nxt = min((t for t in stops_at if t >= s), default=end)
        longest = max(longest, int((nxt - s).total_seconds() // 60))
    peak = max(of("fatigue_peak"), key=lambda e: e.payload.get("value", 0), default=None)
    motion = max(of("motion_peak"), key=lambda e: e.payload.get("value", 0), default=None)
    shown, accepted = len(of("recommendation_shown")), len(of("recommendation_accepted"))
    tips = []
    if longest > 120:
        tips.append("다음엔 2시간이 되기 전에 한 번 쉬어 보세요.")
    if peak is not None and peak.payload.get("value", 0) >= 70:
        tips.append("다음엔 첫 휴식을 20분 앞당겨 보세요.")
    if shown and accepted == 0:
        tips.append("제안이 잦게 느껴지면 설정에서 제안 시점을 '여유'로 바꿔 보세요.")
    return {
        "tripId": trip.id,
        "durationMin": int((end - start).total_seconds() // 60),
        "stops": len(of("stop_started")),
        "longestContinuousDriveMin": longest,
        "fatiguePeak": {"value": peak.payload.get("value"), "at": aware(peak.occurred_at).isoformat()} if peak else None,
        "suggestionsShown": shown,
        "suggestionsAccepted": accepted,
        "motionPeak": {"value": motion.payload.get("value"), "at": aware(motion.occurred_at).isoformat()} if motion else None,
        "waits": len(of("wait_started")),
        "tips": tips[:2],
    }
