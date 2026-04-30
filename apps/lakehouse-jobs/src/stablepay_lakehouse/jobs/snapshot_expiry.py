"""Daily Iceberg snapshot expiration via Trino."""

from __future__ import annotations

import structlog

from stablepay_lakehouse.config import ALL_TABLES, SNAPSHOT_RETAIN_DAYS
from stablepay_lakehouse.trino_client import execute_maintenance

log = structlog.get_logger()


def run_snapshot_expiry() -> None:
    log.info("snapshot_expiry_started", table_count=len(ALL_TABLES), retain_days=SNAPSHOT_RETAIN_DAYS)
    for table in ALL_TABLES:
        sql = f"ALTER TABLE iceberg.{table} EXECUTE expire_snapshots(retention_threshold => '{SNAPSHOT_RETAIN_DAYS}d')"
        try:
            execute_maintenance(sql, f"expire snapshots {table}")
        except Exception:
            log.exception("snapshot_expiry_table_failed", table=table)
    log.info("snapshot_expiry_finished")
