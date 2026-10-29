"""content/*.json(검수본)과 content/drafts/*.json(초안)을 DB에 적재한다.

API는 reviewed=true만 내보내므로 초안을 적재해도 서비스되지 않는다(test_plan T5-8). 초안은 검수 화면·통계용.
사용: cd server → .venv\\Scripts\\python -m app.jobs.load_content
"""
import json
import logging
from pathlib import Path

from sqlalchemy import Engine
from sqlalchemy.orm import Session

from app.models import ContentEpisode, Mission

log = logging.getLogger(__name__)
CONTENT_DIR = Path(__file__).resolve().parents[3] / "content"


def _items(path: Path) -> list[dict]:
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8")).get("items", [])


def load(engine: Engine, content_dir: Path = CONTENT_DIR) -> tuple[int, int]:
    """검수본이 초안보다 우선한다(같은 ID). 파일의 reviewed 값을 그대로 쓴다. 반환: (에피소드 수, 미션 수)."""
    episodes = {e["id"]: e for e in _items(content_dir / "drafts" / "episodes.json")}
    episodes.update({e["id"]: e for e in _items(content_dir / "episodes.json")})
    missions = {m["id"]: m for m in _items(content_dir / "drafts" / "missions.json")}
    missions.update({m["id"]: m for m in _items(content_dir / "missions.json")})
    with Session(engine) as s, s.begin():
        for e in episodes.values():
            s.merge(ContentEpisode(
                id=e["id"], topic=e["topic"], title=e["title"], script="\n".join(e.get("lines", [])),
                quiz=e.get("quiz"), duration_sec=e["durationSec"], min_age=e.get("minAge", 4), max_age=e.get("maxAge", 12),
                reviewed=bool(e.get("reviewed", False)),
            ))
        for m in missions.values():
            s.merge(Mission(
                id=m["id"], context=m["context"], title=m["title"], instructions=m["instructions"],
                duration_sec=m["durationSec"], min_age=m.get("minAge", 4), reviewed=bool(m.get("reviewed", False)),
            ))
    log.info("content loaded episodes=%d missions=%d", len(episodes), len(missions))
    return len(episodes), len(missions)


if __name__ == "__main__":
    from app.config import get_settings
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    load(make_engine(get_settings().database_url))
