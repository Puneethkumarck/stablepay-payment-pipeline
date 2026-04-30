"""Tests for catalog backup retention helpers."""

from __future__ import annotations

from pathlib import Path

from stablepay_lakehouse.jobs import catalog_backup


def test_cleanup_old_backups_retains_newest_n(tmp_path: Path) -> None:
    # given — 10 backup files with sortable timestamp suffixes
    for i in range(10):
        (tmp_path / f"iceberg_catalog_2026010{i}T000000Z.sql").write_text("dummy")

    # when
    catalog_backup._cleanup_old_backups(tmp_path)

    # then — only the newest BACKUP_RETAIN_COUNT remain
    remaining = sorted(p.name for p in tmp_path.glob("iceberg_catalog_*.sql"))
    assert len(remaining) == catalog_backup.BACKUP_RETAIN_COUNT
    assert remaining == [
        f"iceberg_catalog_2026010{i}T000000Z.sql" for i in range(10 - catalog_backup.BACKUP_RETAIN_COUNT, 10)
    ]


def test_cleanup_old_backups_noop_below_retention(tmp_path: Path) -> None:
    # given — fewer files than retention threshold
    for i in range(3):
        (tmp_path / f"iceberg_catalog_2026010{i}T000000Z.sql").write_text("dummy")

    # when
    catalog_backup._cleanup_old_backups(tmp_path)

    # then — no files deleted
    assert len(list(tmp_path.glob("iceberg_catalog_*.sql"))) == 3


def test_cleanup_ignores_unrelated_files(tmp_path: Path) -> None:
    # given — mix of backup and other files
    (tmp_path / "iceberg_catalog_20260101T000000Z.sql").write_text("dummy")
    (tmp_path / "other.sql").write_text("dummy")
    (tmp_path / "iceberg_catalog.txt").write_text("dummy")

    # when
    catalog_backup._cleanup_old_backups(tmp_path)

    # then — only the .sql backup file pattern is considered, others untouched
    assert (tmp_path / "other.sql").exists()
    assert (tmp_path / "iceberg_catalog.txt").exists()
    assert (tmp_path / "iceberg_catalog_20260101T000000Z.sql").exists()
