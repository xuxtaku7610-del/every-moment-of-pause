"""경로 좌표열 기준 거리 계산. 수 km 단위라 구간별 등장방형 근사로 충분하다."""
import math
from dataclasses import dataclass

EARTH_M = 6_371_000.0


@dataclass(frozen=True)
class Projection:
    along_m: float  # 경로 시작점부터 투영점까지 경로 거리
    offset_m: float  # 경로까지 수직 거리
    behind: bool  # 첫 점보다 뒤(진행 반대 방향)


def _xy(lat: float, lng: float, lat0: float) -> tuple[float, float]:
    k = math.pi / 180 * EARTH_M
    return lng * k * math.cos(math.radians(lat0)), lat * k


# 고속도로 방향 표기에 쓰이는 종점 도시 (시청 부근 좌표). 앱 RouteGeo.TERMINALS와 같다.
TERMINALS: dict[str, tuple[float, float]] = {
    "서울": (37.5665, 126.9780),
    "부산": (35.1796, 129.0756),
    "인천": (37.4563, 126.7052),
    "대전": (36.3504, 127.3845),
    "대구": (35.8714, 128.6014),
    "광주": (35.1595, 126.8526),
    "목포": (34.8118, 126.3922),
    "강릉": (37.7519, 128.8761),
    "춘천": (37.8813, 127.7298),
    "순천": (34.9507, 127.4872),
}


def _dist(a: tuple[float, float], b: tuple[float, float]) -> float:
    lat0 = (a[0] + b[0]) / 2
    ax, ay = _xy(a[0], a[1], lat0)
    bx, by = _xy(b[0], b[1], lat0)
    return math.hypot(bx - ax, by - ay)


def length(points: list[tuple[float, float]]) -> float:
    """경로 좌표열 길이(m)."""
    return sum(_dist(a, b) for a, b in zip(points, points[1:]))


def heading_toward(points: list[tuple[float, float]], direction: str | None) -> bool | None:
    """방향 표기(종점 이름)가 진행 방향과 맞는지. 경로 끝이 시작보다 종점에 가까우면 True. 종점을 모르면 None."""
    if not direction or len(points) < 2:
        return None
    t = TERMINALS.get(direction.removesuffix("방향").strip())
    if t is None:
        return None
    return _dist(points[-1], t) < _dist(points[0], t)


def project(points: list[tuple[float, float]], lat: float, lng: float) -> Projection:
    """점을 경로(polyline)에 투영해 가장 가까운 구간의 경로 거리와 수직 거리를 구한다."""
    best: Projection | None = None
    along = 0.0
    for i, ((a_lat, a_lng), (b_lat, b_lng)) in enumerate(zip(points, points[1:])):
        lat0 = (a_lat + b_lat) / 2
        ax, ay = _xy(a_lat, a_lng, lat0)
        bx, by = _xy(b_lat, b_lng, lat0)
        px, py = _xy(lat, lng, lat0)
        dx, dy = bx - ax, by - ay
        seg = math.hypot(dx, dy)
        t_raw = ((px - ax) * dx + (py - ay) * dy) / (seg * seg) if seg > 0 else 0.0
        t = min(1.0, max(0.0, t_raw))
        offset = math.hypot(px - (ax + t * dx), py - (ay + t * dy))
        if best is None or offset < best.offset_m:
            best = Projection(along + t * seg, offset, behind=(i == 0 and t_raw < 0))
        along += seg
    assert best is not None
    return best
