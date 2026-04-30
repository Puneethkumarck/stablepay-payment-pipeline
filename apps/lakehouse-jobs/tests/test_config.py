"""Tests for static config invariants."""

from __future__ import annotations

from stablepay_lakehouse.config import (
    ALL_NAMESPACES,
    ALL_TABLES,
    ORPHAN_OLDER_THAN_DAYS,
)


def test_all_tables_use_known_namespace() -> None:
    for table in ALL_TABLES:
        namespace, _, name = table.partition(".")
        assert namespace in ALL_NAMESPACES, f"{table} uses unknown namespace {namespace}"
        assert name, f"{table} has empty table name"


def test_all_tables_count_matches_18() -> None:
    assert len(ALL_TABLES) == 18


def test_all_tables_unique() -> None:
    assert len(set(ALL_TABLES)) == len(ALL_TABLES)


def test_orphan_retention_meets_trino_minimum() -> None:
    # Trino's iceberg.remove-orphan-files.min-retention defaults to 7 days.
    assert ORPHAN_OLDER_THAN_DAYS >= 7
