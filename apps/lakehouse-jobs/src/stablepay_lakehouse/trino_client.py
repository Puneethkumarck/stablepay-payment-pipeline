"""Trino connection and maintenance SQL execution."""

from __future__ import annotations

import time

import structlog
import trino

from stablepay_lakehouse.config import TRINO_CATALOG, TRINO_HOST, TRINO_PORT, TRINO_USER

log = structlog.get_logger()


def get_connection() -> trino.dbapi.Connection:
    return trino.dbapi.connect(
        host=TRINO_HOST,
        port=TRINO_PORT,
        user=TRINO_USER,
        catalog=TRINO_CATALOG,
    )


def execute_maintenance(sql: str, description: str) -> None:
    start = time.monotonic()
    with get_connection() as conn, conn.cursor() as cursor:
        cursor.execute(sql)
        cursor.fetchall()
    elapsed = time.monotonic() - start
    log.info("maintenance_completed", description=description, elapsed_s=round(elapsed, 2))


def list_tables(namespace: str) -> list[str]:
    with get_connection() as conn, conn.cursor() as cursor:
        cursor.execute(f"SHOW TABLES FROM {TRINO_CATALOG}.{namespace}")
        return [row[0] for row in cursor.fetchall()]
