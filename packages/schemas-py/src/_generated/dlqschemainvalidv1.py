"""Auto-generated from dlq/dlq_schema_invalid.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class DlqSchemaInvalidV1:
    """io.stablepay.events.dlq.DlqSchemaInvalidV1"""

    envelope: str
    source_topic: str
    source_partition: int
    source_offset: int
    error_class: str
    error_message: str
    original_payload_bytes: bytes
    failed_at: int
