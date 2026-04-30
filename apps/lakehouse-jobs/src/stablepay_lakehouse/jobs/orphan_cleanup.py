"""Weekly orphan file removal from Iceberg tables via Trino."""

from __future__ import annotations

import structlog

from stablepay_lakehouse.config import ALL_TABLES, ORPHAN_OLDER_THAN_DAYS
from stablepay_lakehouse.trino_client import execute_maintenance

log = structlog.get_logger()


def run_orphan_cleanup() -> None:
    log.info("orphan_cleanup_started", table_count=len(ALL_TABLES), older_than_days=ORPHAN_OLDER_THAN_DAYS)
    for table in ALL_TABLES:
        threshold = f"{ORPHAN_OLDER_THAN_DAYS}d"
        sql = f"ALTER TABLE iceberg.{table} EXECUTE remove_orphan_files(retention_threshold => '{threshold}')"
        try:
            execute_maintenance(sql, f"remove orphans {table}")
        except Exception:
            log.exception("orphan_cleanup_table_failed", table=table)
    log.info("orphan_cleanup_finished")
