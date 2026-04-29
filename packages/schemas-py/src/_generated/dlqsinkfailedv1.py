"""Auto-generated from dlq/dlq_sink_failed.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class DlqSinkFailedV1:
    """io.stablepay.events.dlq.DlqSinkFailedV1"""

    envelope: str
    source_topic: str
    source_partition: int
    source_offset: int
    error_class: str
    error_message: str
    original_payload_bytes: bytes
    failed_at: int
    sink_type: str
    retry_count: int
