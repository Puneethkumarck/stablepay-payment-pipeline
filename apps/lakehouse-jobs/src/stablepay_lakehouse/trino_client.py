"""Trino connection and maintenance SQL execution."""

from __future__ import annotations

import time

import structlog
import trino

from stablepay_lakehouse.config import TRINO_CATALOG, TRINO_HOST, TRINO_PORT

log = structlog.get_logger()


def get_connection() -> trino.dbapi.Connection:
    return trino.dbapi.connect(
        host=TRINO_HOST,
        port=TRINO_PORT,
        user="stablepay",
        catalog=TRINO_CATALOG,
    )


def execute_maintenance(sql: str, description: str) -> None:
    start = time.monotonic()
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute(sql)
        cursor.fetchall()
        elapsed = time.monotonic() - start
        log.info("maintenance_completed", description=description, elapsed_s=round(elapsed, 2))
    except Exception:
        elapsed = time.monotonic() - start
        log.exception("maintenance_failed", description=description, elapsed_s=round(elapsed, 2))
        raise


def list_tables(namespace: str) -> list[str]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(f"SHOW TABLES FROM {TRINO_CATALOG}.{namespace}")
    return [row[0] for row in cursor.fetchall()]
