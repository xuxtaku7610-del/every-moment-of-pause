from sqlalchemy import Engine, create_engine


def make_engine(url: str) -> Engine:
    connect_args = {"connect_timeout": 1} if url.startswith("postgresql") else {}
    return create_engine(url, pool_pre_ping=True, connect_args=connect_args)
