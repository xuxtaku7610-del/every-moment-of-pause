# 동물병원 인허가 원본 파일 자리 (CONCEPT F5)

공공데이터포털 / 지방행정 인허가 데이터 개방에서 **"동물병원"** 파일(CSV)을 받아 이 폴더에 둔다. 파일은 커밋하지 않는다(.gitignore).

```bash
cd server
.venv\Scripts\python -m app.jobs.load_vets
```

- 영업 중인 곳만 적재한다(영업상태명에 "영업" 또는 "정상").
- 위도·경도 열이 있으면 그대로 쓰고, 없으면 `좌표정보(x)`·`좌표정보(y)`를 `VET_SOURCE_CRS`(기본 `EPSG:5174`)에서 WGS84로 바꾼다(`pyproj`). 파일을 받으면 좌표계를 확인한다.
- 파일이 없으면 `app/data/sample_vets.json`(샘플, 전화번호 없음)을 적재한다.
