"""Auto-generated from dlq/dlq_late_events.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class DlqLateEventsV1:
    """io.stablepay.events.dlq.DlqLateEventsV1"""

    envelope: str
    source_topic: str
    source_partition: int
    source_offset: int
    error_class: str
    error_message: str
    original_payload_bytes: bytes
    failed_at: int
    watermark_at: int
    event_time: int
