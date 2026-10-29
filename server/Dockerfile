# 쉼표 API 서버 (COMMERCIAL_READINESS J P0). 빌드 컨텍스트는 저장소 루트 — content/를 함께 넣는다.
#   docker compose up -d --build
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /srv/server

COPY server/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY server/ ./
COPY content/ /srv/content/

# 비밀값(.env)은 이미지에 넣지 않는다 (.dockerignore). 설정은 compose의 environment로 받는다.
EXPOSE 8000
# 마이그레이션 → 휴게소 적재(키 없으면 샘플) → 콘텐츠 적재 → 서버
CMD ["sh", "-c", "alembic upgrade head && python -m app.jobs.ingest_reststops && python -m app.jobs.load_content && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
