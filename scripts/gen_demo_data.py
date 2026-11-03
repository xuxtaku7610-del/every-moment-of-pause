"""시연 데이터 생성기 (같이가개 PIVOT 3단계). 실행: python scripts/gen_demo_data.py [--fetch-osm | --osm-file <Overpass 응답.json>] [--scenario-only]

만드는 것 (결정적):
- scenarios/geo/mountain_road.gpx: 고갯길 형상 = **OpenStreetMap 실제 도로** 지방도 456호선 대관령 옛길 구간
  (대관령 정상 부근 → 강릉 쪽 굽이진 내리막). `--fetch-osm`일 때만 Overpass API에서 다시 받아 쓰고,
  평소에는 저장소의 GPX를 그대로 읽어 길이만 쓴다. © OpenStreetMap contributors, ODbL 1.0
  (https://www.openstreetmap.org/copyright) — GPX 메타데이터·README·앱 정보 화면에 출처를 적는다.
- scenarios/pet-trip.json: 장면 6개(depart·highway·rest_stop·wait·mountain_road·arrive) 시나리오.
- server/app/data/sample_reststops.json: 영동선(강릉 방향) 휴게소 샘플 추가 — 이름은 실제, 좌표는 시연 경로 위 근사, 편의시설은 샘플.
- server/app/data/sample_pet_places.json, sample_vets.json: 실제 지역명 + "[샘플]" 시설. 전화번호는 넣지 않는다.
"""
import json
import math
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EARTH = 6_371_000.0

# 시연 경로(서울 → 영동고속도로 → 대관령 → 강릉)의 큰 꼭짓점. 길이는 내비 총거리 230km로 늘려 쓴다.
MAIN = [
    (37.5665, 126.9780), (37.4800, 127.0700), (37.2900, 127.1100), (37.2600, 127.3000),
    (37.2700, 127.4500), (37.3000, 127.6300), (37.3300, 127.8600), (37.4000, 127.9800),
    (37.4950, 128.0700), (37.5500, 128.2800), (37.5700, 128.4000), (37.6200, 128.6000),
    (37.6880, 128.7570), (37.7519, 128.8761),
]
TOTAL_KM = 230.0
# 고속도로 교통 흐름 (scenario 안 highway())
TRAFFIC_STEP_SEC = 20
TRAFFIC_SWING_KPH = 6.0
TRAFFIC_SLOW_KPH = 85.0
MOUNTAIN_START_KM = 205.0


def dist(a, b):
    lat0 = math.radians((a[0] + b[0]) / 2)
    dx = math.radians(b[1] - a[1]) * math.cos(lat0) * EARTH
    dy = math.radians(b[0] - a[0]) * EARTH
    return math.hypot(dx, dy)


def point_at_km(km):
    """MAIN 위 경로 거리 km(230km 척도) 지점."""
    seg = [dist(a, b) for a, b in zip(MAIN, MAIN[1:])]
    target = km / TOTAL_KM * sum(seg)
    for (a, b), d in zip(zip(MAIN, MAIN[1:]), seg):
        if target <= d:
            t = target / d
            return (round(a[0] + (b[0] - a[0]) * t, 6), round(a[1] + (b[1] - a[1]) * t, 6))
        target -= d
    return MAIN[-1]


def offset(p, north_m, east_m):
    lat = p[0] + math.degrees(north_m / EARTH)
    lng = p[1] + math.degrees(east_m / (EARTH * math.cos(math.radians(p[0]))))
    return (round(lat, 6), round(lng, 6))


# ───────────── 고갯길 형상 (OpenStreetMap) ─────────────

OVERPASS = "https://overpass-api.de/api/interpreter"
# 지방도 456호선(옛 영동고속도로 대관령 구간) — 대관령 정상 부근에서 강릉 쪽 내리막 끝까지.
OSM_QUERY = '[out:json][timeout:60];way["highway"~"^(secondary|primary|tertiary)$"]["ref"="456"](37.64,128.68,37.78,128.92);out tags geom;'
OSM_START_WAY = 177431988  # 정상 부근(서쪽 끝 37.6786, 128.7355)에서 시작하는 구간
OSM_STOP_LNG = 128.83      # 내리막이 끝나는 성산면 쪽 경도
GPX_PATH = ROOT / "scenarios/geo/mountain_road.gpx"


def fetch_overpass():
    """Overpass 응답(JSON). 서버가 붐비면(429·502·504) 조금 쉬었다가 3번까지 다시 묻는다."""
    body = urllib.parse.urlencode({"data": OSM_QUERY}).encode()
    req = urllib.request.Request(OVERPASS, data=body, headers={"User-Agent": "every-moment-of-pause demo data (scripts/gen_demo_data.py)"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=90) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code not in (429, 502, 504) or attempt == 2:
                raise
            time.sleep(10 * (attempt + 1))


def osm_mountain(d):
    """456호선 조각들을 끝점이 맞닿는 순서로 한 줄로 잇는다. (점열, way id 목록, OSM 기준 시각)"""
    ways = {w["id"]: [(p["lat"], p["lon"]) for p in w["geometry"]] for w in d["elements"]}
    pts, used = list(ways[OSM_START_WAY]), [OSM_START_WAY]
    while pts[-1][1] < OSM_STOP_LNG:
        nxt = None
        for wid, g in ways.items():
            if wid in used:
                continue
            for seg in (g, g[::-1]):
                if dist(seg[0], pts[-1]) < 2 and (nxt is None or len(seg) > len(nxt[1])):
                    nxt = (wid, seg)
        if nxt is None:
            break
        used.append(nxt[0])
        pts += nxt[1][1:]
    return pts, used, d["osm3s"]["timestamp_osm_base"]


def write_gpx(points, way_ids, osm_time, path):
    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<gpx version="1.1" creator="every-moment-of-pause scripts/gen_demo_data.py" xmlns="http://www.topografix.com/GPX/1/1">',
        "  <metadata>",
        "    <name>mountain_road (대관령 옛길, 지방도 456호선)</name>",
        "    <desc>OpenStreetMap 실제 도로 형상. 대관령 정상 부근 → 강릉 쪽 내리막. "
        f"OSM way {','.join(map(str, way_ids))} (기준 {osm_time}). 멀미 예보 시연·비교 실험 전용.</desc>",
        '    <author><name>OpenStreetMap contributors</name></author>',
        '    <copyright author="OpenStreetMap contributors"><year>' + osm_time[:4] + "</year>"
        "<license>https://opendatacommons.org/licenses/odbl/1-0/</license></copyright>",
        '    <link href="https://www.openstreetmap.org/copyright"><text>© OpenStreetMap contributors, ODbL 1.0</text></link>',
        "  </metadata>",
        "  <trk><name>mountain_road</name><trkseg>",
    ]
    lines += [f'    <trkpt lat="{la:.7f}" lon="{lo:.7f}"/>' for la, lo in points]
    lines += ["  </trkseg></trk>", "</gpx>", ""]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes("\n".join(lines).encode("utf-8"))


def read_gpx(path):
    text = path.read_text(encoding="utf-8")
    pts = [(float(a), float(b)) for a, b in re.findall(r'<trkpt lat="([-0-9.]+)" lon="([-0-9.]+)"', text)]
    return pts, sum(dist(a, b) for a, b in zip(pts, pts[1:]))


# ───────────── 휴게소 샘플 (영동선 강릉 방향) ─────────────

YEONGDONG = [
    # (코드, 이름, 경로 km, 편의시설) — 이름은 실제 휴게소, 좌표는 시연 경로 위 근사, 편의시설은 샘플
    ("Y01", "용인휴게소", 40, ["ev_fast", "nursing_room"]),
    ("Y02", "덕평자연휴게소", 60, ["pet_zone", "ev_fast", "kids_zone"]),
    ("Y03", "여주휴게소", 85, ["ev_fast"]),
    ("Y04", "문막휴게소", 112, ["ev_fast", "nursing_room"]),
    ("Y05", "횡성휴게소", 140, ["pet_zone", "ev_fast"]),
    ("Y06", "평창휴게소", 170, ["pet_zone", "ev_fast"]),
    ("Y07", "강릉휴게소", 198, ["ev_fast", "kids_zone"]),
]


def update_reststops():
    p = ROOT / "server/app/data/sample_reststops.json"
    data = json.loads(p.read_text(encoding="utf-8"))
    codes = {a["serviceAreaCode"] for a in data["restAreas"]}
    for code, name, km, fac in YEONGDONG:
        lat, lng = point_at_km(km)
        if code not in codes:
            data["restAreas"].append({
                "serviceAreaCode": code, "serviceAreaName": name, "stopType": "service_area",
                "routeName": "영동선", "direction": "강릉", "yValue": lat, "xValue": lng,
                "telNo": None, "svarAddr": None, "approxKmFromSeoul": km,
            })
            data["facilities"] += [{"serviceAreaCode": code, "facilityCode": f, "quantity": 1} for f in fac]
    note = " 영동선(Y01~Y07, 강릉 방향)은 같이가개 시연 경로(scenarios/pet-trip.json) 위 근사 좌표이고, 반려견 놀이터 등 편의시설은 샘플이다(확인 필요)."
    if note not in data["_comment"]:
        data["_comment"] += note
    p.write_bytes((json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))


# ───────────── 반려동물 동반 장소·동물병원 샘플 ─────────────

REGIONS = [  # (지역명, 경로 km)
    ("경기 이천시", 60), ("경기 여주시", 85), ("강원특별자치도 원주시", 112),
    ("강원특별자치도 횡성군", 140), ("강원특별자치도 평창군", 170),
]
GANGNEUNG = (37.7519, 128.8761)
CATS = ["cafe", "restaurant", "walk", "lodging", "tour"]
CAT_KO = {"cafe": "카페", "restaurant": "식당", "walk": "산책로", "lodging": "숙소", "tour": "관광지"}
POLICY = {
    "cafe": "소형견 동반 가능, 실내 이동장 권장 (샘플)",
    "restaurant": "테라스석 반려견 동반 가능 (샘플)",
    "walk": "목줄 착용, 배변봉투 지참 (샘플)",
    "lodging": "반려견 동반 객실 운영, 사전 문의 (샘플)",
    "tour": "야외 구역 반려견 동반 가능 (샘플)",
}


def pet_places():
    items, n = [], 0
    for region, km in REGIONS:
        base = point_at_km(km)
        for i, cat in enumerate(CATS[:3]):
            n += 1
            east = (800 + 700 * i) * (1 if i % 2 == 0 else -1)
            lat, lng = offset(base, 400 * (i - 1), east)
            items.append(_place(n, f"[샘플] {region.split()[-1]} 반려견 동반 {CAT_KO[cat]}", cat, lat, lng, region))
    # 목적지(강릉) 주변 산책로·카페·숙소 — 도착 10분 전 제안용
    for i, (cat, dn, de) in enumerate([("walk", 1800, 1500), ("walk", -1200, 2200), ("cafe", 2100, 1900), ("cafe", 900, 1200),
                                         ("lodging", 2600, 2400), ("restaurant", -600, 900), ("tour", 1800, -1500)]):
        n += 1
        lat, lng = offset(GANGNEUNG, dn, de)
        items.append(_place(n, f"[샘플] 강릉 반려견 동반 {CAT_KO[cat]} {i + 1}", cat, lat, lng, "강원특별자치도 강릉시"))
    return {"_comment": "반려동물 동반여행 샘플. 지역명은 실제, 시설 이름·정책·좌표는 시연용 샘플이다(실재 업소 아님). "
                        "TOUR_API_KEY가 있으면 한국관광공사 TourAPI(반려동물 동반여행) 수집 결과로 바뀐다.",
            "items": items}


def _place(n, name, cat, lat, lng, region):
    return {"sourceId": f"SAMPLE-P{n:03d}", "name": name, "category": cat, "lat": lat, "lng": lng,
            "address": f"{region} (샘플)", "petPolicy": POLICY[cat]}


def vets():
    items, n = [], 0
    places = [("서울특별시", (37.5665, 126.9780))] + [(r, point_at_km(km)) for r, km in REGIONS[2:]] + [("강원특별자치도 강릉시", GANGNEUNG)]
    for region, base in places:
        for j, (dn, de) in enumerate([(1200, -900), (-700, 1600)]):
            n += 1
            lat, lng = offset(base, dn, de)
            items.append({"licenseNo": f"SAMPLE-V{n:03d}", "name": f"[샘플] {region.split()[-1]} 동물병원 {'AB'[j]}",
                          "address": f"{region} (샘플)", "phone": None, "lat": lat, "lng": lng, "open": True})
    return {"_comment": "동물병원 샘플. 지역명은 실제, 병원 이름·좌표는 시연용 샘플이다(실재 병원 아님). 전화번호는 지어내지 않는다. "
                        "server/data/raw/에 지방행정 인허가(동물병원) 파일을 두면 load_vets가 그 파일로 적재한다.",
            "items": items}


# ───────────── 시나리오 ─────────────

def scenario(mountain_len_m):
    """장면 6개. 거리는 230km 척도의 경로 진행 거리로 맞춘다(남은 거리 = 230km − 진행)."""
    total = int(TOTAL_KM * 1000)
    frames = []
    t = 0
    pos = 0.0  # m

    def kf(**kw):
        d = {"t": t}
        d.update(kw)
        d.setdefault("remainDistanceM", round(total - pos))
        d.setdefault("remainTimeSec", round((total - pos) / (90 / 3.6)))
        frames.append(d)

    def drive(to_m, kph):
        nonlocal t, pos
        dt = (to_m - pos) / (kph / 3.6)
        t += int(round(dt))
        pos = to_m

    def highway(to_m, kph):
        """고속도로 교통 흐름: drive()와 같은 시각·거리에 닿되, 그 사이 TRAFFIC_STEP_SEC초마다 속도만 ±TRAFFIC_SWING_KPH 출렁이고
        약 9분마다 1분쯤 TRAFFIC_SLOW_KPH까지 준다(결정적 의사 난수). 실제 고속도로도 차간 거리 조절·앞차 감속으로 흔들린다는 가정 —
        일정 속도 100km/h 그대로면 멀미 지수가 2시간 내내 0이라 화면이 비어 보였다(2026-10-31)."""
        nonlocal t, pos
        t0, p0 = t, pos
        drive(to_m, kph)
        t1, p1 = t, pos
        k = 1
        while t0 + k * TRAFFIC_STEP_SEC < t1 - TRAFFIC_STEP_SEC:
            tt = t0 + k * TRAFFIC_STEP_SEC
            noise = math.sin(k * 12.9898 + t0 * 0.001) * 43758.5453
            noise = (noise - math.floor(noise)) * 2 - 1
            v = kph + TRAFFIC_SWING_KPH * noise
            if k % 27 in (13, 14, 15):
                v = TRAFFIC_SLOW_KPH
            at = p0 + (p1 - p0) * (tt - t0) / (t1 - t0)
            frames.append({"t": tt, "speedKph": round(v, 1), "remainDistanceM": round(total - at), "remainTimeSec": round((total - at) / (90 / 3.6))})
            k += 1

    kf(label="depart", trip="NOT_STARTED", driveState="PARKED", gear="PARK", speedKph=0, socPercent=82, charging=False,
       hvacTempC=23, parkingBrake=None, doorDriver=False, doorRearLeft=False, doorRearRight=False)
    frames[-1].pop("parkingBrake")
    t = 60
    kf(label="출발 체크 완료", trip="ACTIVE")
    t = 90
    kf(label="출발", driveState="DRIVING", gear="DRIVE", speedKph=30)
    drive(1500, 45)
    kf(label="highway", speedKph=100)
    # 고속도로 → 횡성휴게소(140km) 진입 직전 감속
    highway(139_000, 100)
    kf(speedKph=100, socPercent=58)
    drive(139_800, 60)
    kf(label="휴게소 진입", speedKph=30)
    drive(140_000, 30)
    kf(label="rest_stop", driveState="PARKED", gear="PARK", speedKph=0, socPercent=57)
    # 문 이벤트 b) (차량 이벤트 연결 11/02): 정차 20초 뒤 운전석 문 열림 → "잠깐 다녀올게" 제안 한 줄, 10초 뒤 닫힘
    t_stop = t
    t = t_stop + 20
    kf(doorDriver=True, speedKph=0)
    t = t_stop + 30
    kf(doorDriver=False, speedKph=0)
    t = t_stop + 300
    kf(label="wait", hvacTempC=24, speedKph=0)
    t += 900  # 대기 15분 (제한 10분 → 초과 장면)
    kf(label="대기 끝", hvacTempC=25, speedKph=0)  # 정차 끝에 속도 0 (없으면 정차 중 속도가 0→40으로 보간됐다)
    t += 60
    kf(label="다시 출발", driveState="DRIVING", gear="DRIVE", speedKph=40)
    drive(141_000, 60)
    kf(speedKph=100)
    highway(MOUNTAIN_START_KM * 1000 - 1000, 100)
    kf(label="대관령 진입", speedKph=100, socPercent=40)
    drive(MOUNTAIN_START_KM * 1000, 70)
    kf(label="mountain_road", speedKph=45)
    end_mountain = MOUNTAIN_START_KM * 1000 + mountain_len_m
    drive(end_mountain, 45)
    kf(label="고개 아래", speedKph=45, socPercent=34)
    drive(total - 1000, 50)
    kf(label="arrive", speedKph=30)
    drive(total, 30)
    kf(label="도착", trip="ENDED", driveState="PARKED", gear="PARK", speedKph=0, socPercent=33, remainDistanceM=0, remainTimeSec=0, hvacTempC=27)
    # 문 이벤트 (11/02): "내렸나요?"(도착 60초 뒤)가 나온 다음에 둔다 — 리허설(도착 장면)은 그대로.
    # 76초 뒤 뒷문(오른쪽) 열림·닫힘, 96초 뒤 운전석 문 열림 → 답이 없으면 c) 안전 안내 한 번. a)는 발표 모드 수동(`--es door rear`).
    t_end = t
    for dt, door in ((76, {"doorRearRight": True}), (86, {"doorRearRight": False}), (96, {"doorDriver": True}), (106, {"doorDriver": False})):
        t = t_end + dt
        kf(remainDistanceM=0, remainTimeSec=0, **door)
    t = t_end + 120
    kf(label="도착 체크", remainDistanceM=0, remainTimeSec=0)
    return {
        "name": "pet-trip",
        "description": "같이가개 시연: 서울→강릉, 콩이(소형·9살·멀미 성향 높음). depart·highway·rest_stop(횡성)·wait·mountain_road(대관령 옛길, OSM)·arrive.",
        "startClock": "09:00",
        "capabilities": ["SPEED", "GEAR", "BATTERY_SOC", "CHARGING", "HVAC_TEMP", "DOOR"],
        "setup": {"ev": True, "sleepHours": 7,
                  "pet": {"name": "콩이", "size": "SMALL", "ageYears": 9, "motionTendency": "HIGH", "pottyIntervalMin": 90}},
        "route": {
            "destinationName": "강릉",
            "destination": {"lat": MAIN[-1][0], "lng": MAIN[-1][1]},
            "totalDistanceM": total,
            "points": [{"lat": a, "lng": b} for a, b in MAIN],
            "shapeGpx": "geo/mountain_road.gpx",
            "shapeStartM": int(MOUNTAIN_START_KM * 1000),
        },
        "timeline": frames,
    }


def main():
    # --fetch-osm: Overpass에서 받기, --osm-file <경로>: 브라우저 등으로 받아 둔 Overpass JSON 응답 쓰기.
    if "--fetch-osm" in sys.argv or "--osm-file" in sys.argv:
        if "--osm-file" in sys.argv:
            d = json.loads(Path(sys.argv[sys.argv.index("--osm-file") + 1]).read_text(encoding="utf-8"))
        else:
            d = fetch_overpass()
        pts, ways, osm_time = osm_mountain(d)
        write_gpx(pts, ways, osm_time, GPX_PATH)
    pts, length = read_gpx(GPX_PATH)
    sc = scenario(length)
    (ROOT / "scenarios/pet-trip.json").write_bytes((json.dumps(sc, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))
    if "--scenario-only" in sys.argv:  # 시나리오만 다시 만든다(공공데이터로 바꾼 샘플 파일은 건드리지 않음)
        print(f"pet-trip {len(sc['timeline'])} keyframes, duration {sc['timeline'][-1]['t']}s")
        return
    update_reststops()
    for name, data in (("sample_pet_places.json", pet_places()), ("sample_vets.json", vets())):
        (ROOT / "server/app/data" / name).write_bytes((json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))
    scenes = {f["label"]: f["t"] for f in sc["timeline"] if f.get("label") in ("depart", "highway", "rest_stop", "wait", "mountain_road", "arrive")}
    print(f"mountain_road {length:.0f}m, {len(pts)} pts; scenes {scenes}; duration {sc['timeline'][-1]['t']}s")


if __name__ == "__main__":
    main()
