"""test_plan §9: 도로공사 수집 잡"""
import copy
import logging

import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

from app.config import Settings
from app.jobs.ingest_reststops import ingest, load_sample
from app.models import RestStop, RestStopFacility


def counts(engine) -> tuple[int, int]:
    with Session(engine) as s:
        return s.scalar(select(func.count()).select_from(RestStop)), s.scalar(select(func.count()).select_from(RestStopFacility))


def test_empty_key_loads_sample_without_external_call(db_engine, caplog):
    # T9-15 [N] EX_API_KEY 비어 있음 → 외부 호출 없이 sample_reststops.json 적재, 로그 source=sample
    def no_fetch(_key):
        raise AssertionError("외부 호출 금지")

    caplog.set_level(logging.INFO)
    r = ingest(db_engine, Settings(ex_api_key=""), fetch=no_fetch)
    assert (r.source, r.ok, r.inserted) == ("sample", True, 23)
    assert "source=sample" in caplog.text
    stops, facilities = counts(db_engine)
    assert stops == 23 and facilities > 50
    again = ingest(db_engine, Settings(ex_api_key=""))
    assert (again.inserted, again.updated, again.deleted) == (0, 0, 0)
    assert counts(db_engine) == (stops, facilities)


def test_new_changed_deleted_are_applied(db_engine):
    # [N] 신규·변경·삭제 휴게소 반영
    ingest(db_engine, Settings(ex_api_key=""))
    payload = copy.deepcopy(load_sample())
    payload["restAreas"][0]["serviceAreaName"] = "서울만남의광장휴게소(새 이름)"
    removed = payload["restAreas"].pop(1)["serviceAreaCode"]
    payload["restAreas"].append({**payload["restAreas"][0], "serviceAreaCode": "S9999", "serviceAreaName": "새휴게소"})
    payload["facilities"] = [f for f in payload["facilities"] if f["serviceAreaCode"] != "G01"] + [
        {"serviceAreaCode": "G01", "facilityCode": "kids_zone", "quantity": 1},
        {"serviceAreaCode": "G01", "facilityCode": "unknown_code", "quantity": 1},
    ]
    r = ingest(db_engine, Settings(ex_api_key="k"), fetch=lambda _k: payload)
    assert (r.source, r.inserted, r.updated, r.deleted) == ("ex", 1, 1, 1)
    with Session(db_engine) as s:
        assert s.get(RestStop, "rs_g01").name == "서울만남의광장휴게소(새 이름)"
        assert s.scalar(select(RestStop).where(RestStop.ex_code == removed)) is None
        codes = set(s.scalars(select(RestStopFacility.facility_code).where(RestStopFacility.rest_stop_id == "rs_g01")))
        assert codes == {"kids_zone"}


def test_api_failure_keeps_existing_data(db_engine, caplog):
    # [N] API 실패 시 기존 데이터 유지
    ingest(db_engine, Settings(ex_api_key=""))
    before = counts(db_engine)

    def boom(_key):
        raise TimeoutError

    r = ingest(db_engine, Settings(ex_api_key="k"), fetch=boom)
    assert r.ok is False
    assert counts(db_engine) == before
    assert "failed" in caplog.text


def test_malformed_rows_are_skipped(db_engine):
    payload = {"restAreas": [
        {"serviceAreaCode": "A1", "serviceAreaName": "정상휴게소", "yValue": 37.0, "xValue": 127.0},
        {"serviceAreaCode": "A2", "serviceAreaName": "좌표없음"},
        {"serviceAreaCode": "A3", "serviceAreaName": "범위밖", "yValue": 137.0, "xValue": 127.0},
        {"serviceAreaCode": "A4", "serviceAreaName": "종류이상", "yValue": 37.0, "xValue": 127.0, "stopType": "mall"},
    ]}
    r = ingest(db_engine, Settings(ex_api_key="k"), fetch=lambda _k: payload)
    assert r.inserted == 1


@pytest.mark.pg
def test_sample_ingest_on_postgres(pg_url):
    from alembic import command
    from alembic.config import Config

    cfg = Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", pg_url)
    command.upgrade(cfg, "head")
    engine = create_engine(pg_url)
    try:
        with Session(engine) as s, s.begin():
            s.query(RestStopFacility).delete()
            s.query(RestStop).delete()
        r = ingest(engine, Settings(ex_api_key=""))
        assert r.inserted == 23
        assert counts(engine)[0] == 23
    finally:
        engine.dispose()
