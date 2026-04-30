"""APScheduler entrypoint for Iceberg maintenance jobs."""

from __future__ import annotations

import signal
import sys

import structlog
from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger

from stablepay_lakehouse.jobs.catalog_backup import run_catalog_backup
from stablepay_lakehouse.jobs.compaction import run_compaction
from stablepay_lakehouse.jobs.orphan_cleanup import run_orphan_cleanup
from stablepay_lakehouse.jobs.snapshot_expiry import run_snapshot_expiry

log = structlog.get_logger()


def main() -> None:
    structlog.configure(
        processors=[
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.processors.JSONRenderer(),
        ],
    )

    scheduler = BlockingScheduler()

    scheduler.add_job(
        run_compaction,
        CronTrigger(minute=0),
        id="compaction",
        max_instances=1,
    )
    scheduler.add_job(
        run_catalog_backup,
        CronTrigger(hour=1, minute=0),
        id="catalog_backup",
        max_instances=1,
    )
    scheduler.add_job(
        run_snapshot_expiry,
        CronTrigger(hour=2, minute=0),
        id="snapshot_expiry",
        max_instances=1,
    )
    scheduler.add_job(
        run_orphan_cleanup,
        CronTrigger(day_of_week=0, hour=3, minute=0),
        id="orphan_cleanup",
        max_instances=1,
    )

    def shutdown(signum: int, _frame: object) -> None:
        log.info("shutdown_requested", signal=signal.Signals(signum).name)
        scheduler.shutdown(wait=False)
        sys.exit(0)

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    log.info(
        "scheduler_started",
        jobs={
            "compaction": "every hour at :00",
            "catalog_backup": "daily at 01:00",
            "snapshot_expiry": "daily at 02:00",
            "orphan_cleanup": "weekly Monday 03:00",
        },
    )
    scheduler.start()
