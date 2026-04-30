"""Daily Postgres Iceberg catalog backup via pg_dump."""

from __future__ import annotations

import os
import subprocess
import time
from datetime import UTC, datetime
from pathlib import Path

import structlog

from stablepay_lakehouse.config import (
    BACKUP_DIR,
    POSTGRES_HOST,
    POSTGRES_PASSWORD,
    POSTGRES_PORT,
    POSTGRES_USER,
)

log = structlog.get_logger()

BACKUP_RETAIN_COUNT = 7


def run_catalog_backup() -> None:
    start = time.monotonic()
    backup_path = Path(BACKUP_DIR)
    backup_path.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now(tz=UTC).strftime("%Y%m%dT%H%M%SZ")
    filename = f"iceberg_catalog_{timestamp}.sql"
    filepath = backup_path / filename

    env = {**os.environ, "PGPASSWORD": POSTGRES_PASSWORD}
    cmd = [
        "pg_dump",
        "-h",
        POSTGRES_HOST,
        "-p",
        str(POSTGRES_PORT),
        "-U",
        POSTGRES_USER,
        "-d",
        "iceberg_catalog",
        "-f",
        str(filepath),
    ]

    try:
        subprocess.run(cmd, env=env, check=True, capture_output=True, text=True)
        size_bytes = filepath.stat().st_size
        elapsed = time.monotonic() - start
        log.info(
            "catalog_backup_completed",
            file=str(filepath),
            size_bytes=size_bytes,
            elapsed_s=round(elapsed, 2),
        )
    except subprocess.CalledProcessError as exc:
        elapsed = time.monotonic() - start
        log.exception(
            "catalog_backup_failed",
            elapsed_s=round(elapsed, 2),
            stderr=exc.stderr,
        )
        raise

    _cleanup_old_backups(backup_path)


def _cleanup_old_backups(backup_path: Path) -> None:
    backups = sorted(backup_path.glob("iceberg_catalog_*.sql"), reverse=True)
    for old in backups[BACKUP_RETAIN_COUNT:]:
        old.unlink()
        log.info("old_backup_deleted", file=str(old))
