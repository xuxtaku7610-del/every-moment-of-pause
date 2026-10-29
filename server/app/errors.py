"""api_spec §0 오류 형식: {"error": {"code", "message", "detail"}}."""
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


class ApiError(Exception):
    def __init__(self, status: int, code: str, message: str, detail: dict[str, Any] | None = None,
                 extra: dict[str, Any] | None = None) -> None:
        self.status = status
        self.code = code
        self.message = message
        self.detail = detail
        self.extra = extra or {}


def error_body(code: str, message: str, detail: dict[str, Any] | None = None) -> dict[str, Any]:
    err: dict[str, Any] = {"code": code, "message": message}
    if detail:
        err["detail"] = detail
    return {"error": err}


def install(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def _api_error(_: Request, e: ApiError) -> JSONResponse:
        return JSONResponse({**error_body(e.code, e.message, e.detail), **e.extra}, status_code=e.status)

    @app.exception_handler(RequestValidationError)
    async def _validation(_: Request, e: RequestValidationError) -> JSONResponse:
        fields = [".".join(str(p) for p in err["loc"][1:]) for err in e.errors()]
        return JSONResponse(error_body("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", {"fields": fields}), status_code=422)
