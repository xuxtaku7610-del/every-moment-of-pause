"""로그에 비밀 쿼리 값이 남지 않게 가린다 (결정 10/30).

공공데이터포털 API는 키를 쿼리스트링 `serviceKey=`로 받는다. httpx 같은 라이브러리는 INFO 로그에 요청 URL을 그대로 찍으므로,
모든 로그 레코드의 메시지와 인자에서 `serviceKey`·`key`·`token` 쿼리 값을 `***`로 바꾼다.
LogRecord 팩토리를 감싸므로 어느 로거·핸들러로 가든(파일·콘솔·JSON) 같은 규칙이 적용된다.
"""
import logging
import re
from typing import Any

# serviceKey=..., ServiceKey=..., api_key=..., key=...(쿼리스트링·URL 인코딩 포함)
_SECRET = re.compile(r"(?i)\b((?:service_?key|api_?key|key|access_?token))=([^&\s\"'#]+)")
MASK = "***"

_installed = False


def mask(text: str) -> str:
    return _SECRET.sub(lambda m: f"{m.group(1)}={MASK}", text)


def _clean(v: Any) -> Any:
    if isinstance(v, (int, float, bool)) or v is None:
        return v
    s = str(v)
    m = mask(s)
    return m if m != s else v


def install() -> None:
    """한 번만 설치한다 (여러 번 불러도 같다)."""
    global _installed
    if _installed:
        return
    previous = logging.getLogRecordFactory()

    def factory(*args: Any, **kwargs: Any) -> logging.LogRecord:
        record = previous(*args, **kwargs)
        if isinstance(record.msg, str):
            record.msg = mask(record.msg)
        if isinstance(record.args, tuple):
            record.args = tuple(_clean(a) for a in record.args)
        elif isinstance(record.args, dict):
            record.args = {k: _clean(v) for k, v in record.args.items()}
        return record

    logging.setLogRecordFactory(factory)
    _installed = True
