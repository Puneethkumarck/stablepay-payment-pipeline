"""Environment-driven configuration for lakehouse maintenance jobs."""

from __future__ import annotations

import os

TRINO_HOST = os.getenv("STBLPAY_TRINO_HOST", "trino")
TRINO_PORT = int(os.getenv("STBLPAY_TRINO_PORT", "8080"))
TRINO_CATALOG = "iceberg"

POSTGRES_HOST = os.getenv("POSTGRES_HOST", "postgres")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
POSTGRES_USER = os.getenv("POSTGRES_USER", "stablepay")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "stablepay_dev")

COMPACTION_TARGET_SIZE_MB = int(os.getenv("COMPACTION_TARGET_SIZE_MB", "128"))
SNAPSHOT_RETAIN_DAYS = int(os.getenv("SNAPSHOT_RETAIN_DAYS", "7"))
SNAPSHOT_RETAIN_COUNT = int(os.getenv("SNAPSHOT_RETAIN_COUNT", "10"))
ORPHAN_OLDER_THAN_DAYS = int(os.getenv("ORPHAN_OLDER_THAN_DAYS", "3"))
BACKUP_DIR = os.getenv("BACKUP_DIR", "/data/backups")

ALL_NAMESPACES = ["raw", "facts", "agg", "dlq"]

ALL_TABLES = [
    "raw.raw_payment_payout_fiat",
    "raw.raw_payment_payout_crypto",
    "raw.raw_payment_payin_fiat",
    "raw.raw_payment_payin_crypto",
    "raw.raw_payment_flow",
    "raw.raw_chain_transaction",
    "raw.raw_signing_request",
    "raw.raw_screening_result",
    "raw.raw_approval_decision",
    "facts.fact_transactions",
    "facts.fact_flows",
    "facts.fact_screening_outcomes",
    "agg.agg_volume_hourly",
    "agg.agg_success_rate_hourly",
    "agg.agg_screening_outcomes_daily",
    "agg.agg_dlq_summary_hourly",
    "agg.agg_stuck_withdrawals",
    "dlq.dlq_events",
]
