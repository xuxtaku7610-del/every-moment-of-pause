"""GET /content/episodes, POST /content/playlists, GET /content/missions — api_spec §2.5~2.7.

아이용 콘텐츠는 검수된 것(reviewed=true)만 내보낸다 (CLAUDE.md 규칙 4, test_plan T5-8).
"""
import secrets
from typing import Literal

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, require_device
from app.content import Item, plan_playlist
from app.models import ContentEpisode, Mission, Playlist, PlaylistItem

router = APIRouter(dependencies=[Depends(require_device)])
Topic = Literal["proverb", "common_sense", "safety", "manners"]


def _episode(e: ContentEpisode) -> dict:
    return {
        "episodeId": e.id, "topic": e.topic, "title": e.title, "durationSec": e.duration_sec,
        "minAge": e.min_age, "maxAge": e.max_age, "script": e.script, "quiz": e.quiz,
    }


def _reviewed(session: Session, topics: list[str] | None, age: int | None, max_seconds: int | None) -> list[ContentEpisode]:
    q = select(ContentEpisode).where(ContentEpisode.reviewed.is_(True))
    if topics:
        q = q.where(ContentEpisode.topic.in_(topics))
    if age is not None:
        q = q.where(ContentEpisode.min_age <= age, ContentEpisode.max_age >= age)
    if max_seconds is not None:
        q = q.where(ContentEpisode.duration_sec <= max_seconds)
    return list(session.scalars(q.order_by(ContentEpisode.id)))


@router.get("/content/episodes")
def episodes(
    topic: Topic | None = None,
    age: int | None = Query(default=None, ge=0, le=18),
    maxSeconds: int | None = Query(default=None, gt=0),
    session: Session = Depends(get_session),
) -> dict:
    items = _reviewed(session, [topic] if topic else None, age, maxSeconds)
    return {"items": [_episode(e) for e in items]}


class PlaylistIn(BaseModel):
    targetSeconds: int = Field(gt=0, le=12 * 3600)
    topics: list[Topic] = Field(default_factory=list)
    age: int | None = Field(default=None, ge=0, le=18)
    excludeEpisodeIds: list[str] = Field(default_factory=list, max_length=200)


@router.post("/content/playlists", status_code=200)
def playlist(body: PlaylistIn, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    pool = [e for e in _reviewed(session, body.topics or None, body.age, None) if e.id not in set(body.excludeEpisodeIds)]
    chosen = plan_playlist([Item(e.id, e.duration_sec) for e in pool], body.targetSeconds)
    pid = f"pl_{secrets.token_hex(4)}"
    total = sum(i.duration_sec for i in chosen)
    session.add(Playlist(id=pid, device_id=device_id, target_sec=body.targetSeconds, total_sec=total))
    session.flush()
    session.add_all(PlaylistItem(playlist_id=pid, position=n, episode_id=i.id) for n, i in enumerate(chosen))
    session.commit()
    return {"playlistId": pid, "totalSec": total, "items": [{"episodeId": i.id, "durationSec": i.duration_sec} for i in chosen]}


@router.get("/content/missions")
def missions(
    context: Literal["rest_stop", "charging"] | None = None,
    age: int | None = Query(default=None, ge=0, le=18),
    maxSeconds: int | None = Query(default=None, gt=0),
    session: Session = Depends(get_session),
) -> dict:
    q = select(Mission).where(Mission.reviewed.is_(True))
    if context:
        q = q.where(Mission.context == context)
    if age is not None:
        q = q.where(Mission.min_age <= age)
    if maxSeconds is not None:
        q = q.where(Mission.duration_sec <= maxSeconds)
    return {"items": [
        {"missionId": m.id, "context": m.context, "title": m.title, "instructions": m.instructions, "durationSec": m.duration_sec, "minAge": m.min_age}
        for m in session.scalars(q.order_by(Mission.id))
    ]}
