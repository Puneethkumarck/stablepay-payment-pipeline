"""Reads DLQ events from the Iceberg dlq_events table."""

from __future__ import annotations

from typing import Any

from pyiceberg.catalog.sql import SqlCatalog


def _catalog() -> SqlCatalog:
    import os

    return SqlCatalog(
        "iceberg_catalog",
        **{
            "uri": os.getenv(
                "STBLPAY_ICEBERG_JDBC_URI",
                "postgresql+psycopg2://stablepay:stablepay_dev@localhost:5432/iceberg_catalog",
            ),
            "warehouse": "s3://warehouse/",
            "s3.endpoint": os.getenv("STBLPAY_S3_ENDPOINT", "http://localhost:9000"),
            "s3.access-key-id": os.getenv("MINIO_ROOT_USER", "minioadmin"),
            "s3.secret-access-key": os.getenv("MINIO_ROOT_PASSWORD", "minioadmin123"),
        },
    )


def list_dlq_events(
    *,
    error_class: str | None = None,
    source_topic: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    catalog = _catalog()
    table = catalog.load_table("dlq.dlq_events")
    scan = table.scan(limit=limit)

    if error_class:
        scan = scan.filter(f"error_class == '{error_class}'")
    if source_topic:
        scan = scan.filter(f"source_topic == '{source_topic}'")

    df = scan.to_pandas()
    return df.to_dict(orient="records")


def get_dlq_event(event_id: str) -> dict[str, Any] | None:
    catalog = _catalog()
    table = catalog.load_table("dlq.dlq_events")
    scan = table.scan().filter(f"event_id == '{event_id}'")
    df = scan.to_pandas()
    if df.empty:
        return None
    return df.iloc[0].to_dict()
