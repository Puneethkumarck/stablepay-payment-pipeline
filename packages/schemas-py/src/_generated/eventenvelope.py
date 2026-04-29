"""Auto-generated from common/event_envelope.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class EventEnvelope:
    """io.stablepay.events.common.EventEnvelope"""

    event_id: str
    event_time: int
    ingest_time: int
    schema_version: str
    flow_id: Optional[str] = None
    correlation_id: Optional[str] = None
    trace_id: Optional[str] = None
