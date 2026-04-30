"""Hourly Iceberg table compaction via Trino optimize."""

from __future__ import annotations

import structlog

from stablepay_lakehouse.config import ALL_TABLES, COMPACTION_TARGET_SIZE_MB
from stablepay_lakehouse.trino_client import execute_maintenance

log = structlog.get_logger()


def run_compaction() -> None:
    log.info("compaction_started", table_count=len(ALL_TABLES), target_size_mb=COMPACTION_TARGET_SIZE_MB)
    for table in ALL_TABLES:
        sql = f"ALTER TABLE iceberg.{table} EXECUTE optimize(file_size_threshold => '{COMPACTION_TARGET_SIZE_MB}MB')"
        try:
            execute_maintenance(sql, f"compact {table}")
        except Exception:
            log.exception("compaction_table_failed", table=table)
    log.info("compaction_finished")
