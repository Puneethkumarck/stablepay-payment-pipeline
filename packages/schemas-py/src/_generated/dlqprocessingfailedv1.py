"""Auto-generated from dlq/dlq_processing_failed.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class DlqProcessingFailedV1:
    """io.stablepay.events.dlq.DlqProcessingFailedV1"""

    envelope: str
    source_topic: str
    source_partition: int
    source_offset: int
    error_class: str
    error_message: str
    original_payload_bytes: bytes
    failed_at: int
    retry_count: int
    last_retry_at: Optional[int] = None
