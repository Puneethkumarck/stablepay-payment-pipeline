"""Reads DLQ events from the Iceberg dlq_events table."""

from __future__ import annotations

import os
from functools import lru_cache
from typing import Any

from pyiceberg.catalog.sql import SqlCatalog
from pyiceberg.expressions import And, BooleanExpression, EqualTo

_TABLE = "dlq.dlq_events"


@lru_cache(maxsize=1)
def _catalog() -> SqlCatalog:
    uri = os.getenv("STBLPAY_ICEBERG_JDBC_URI")
    if not uri:
        raise RuntimeError(
            "STBLPAY_ICEBERG_JDBC_URI is not set; export it (e.g. "
            "postgresql+psycopg2://stablepay:<password>@localhost:5432/iceberg_catalog)"
        )

    return SqlCatalog(
        "iceberg_catalog",
        **{
            "uri": uri,
            "warehouse": os.getenv("STBLPAY_ICEBERG_WAREHOUSE", "s3://warehouse/"),
            "s3.endpoint": os.getenv("STBLPAY_S3_ENDPOINT", "http://localhost:9000"),
            "s3.access-key-id": os.getenv("MINIO_ROOT_USER", "minioadmin"),
            "s3.secret-access-key": os.getenv("MINIO_ROOT_PASSWORD", "minioadmin123"),
        },
    )


def _and_filters(filters: list[BooleanExpression]) -> BooleanExpression | None:
    if not filters:
        return None
    expr = filters[0]
    for f in filters[1:]:
        expr = And(expr, f)
    return expr


def list_dlq_events(
    *,
    error_class: str | None = None,
    source_topic: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    table = _catalog().load_table(_TABLE)

    filters: list[BooleanExpression] = []
    if error_class:
        filters.append(EqualTo("error_class", error_class))
    if source_topic:
        filters.append(EqualTo("source_topic", source_topic))

    row_filter = _and_filters(filters)
    scan = table.scan(row_filter=row_filter, limit=limit) if row_filter else table.scan(limit=limit)
    df = scan.to_pandas()
    return df.to_dict(orient="records")


def get_dlq_event(event_id: str) -> dict[str, Any] | None:
    table = _catalog().load_table(_TABLE)
    df = table.scan(row_filter=EqualTo("event_id", event_id), limit=1).to_pandas()
    if df.empty:
        return None
    return df.iloc[0].to_dict()
