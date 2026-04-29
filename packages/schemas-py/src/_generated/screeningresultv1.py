"""Auto-generated from screening/screening_result.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class ScreeningResultV1:
    """io.stablepay.events.screening.ScreeningResultV1"""

    envelope: str
    screening_id: str
    transaction_reference: str
    customer_id: str
    outcome: str
    provider: Optional[str] = None
    risk_score: Optional[float] = None
    notes: Optional[str] = None
