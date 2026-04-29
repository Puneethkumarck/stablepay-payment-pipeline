"""Auto-generated from flow/payment_flow.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class PaymentFlowV1:
    """io.stablepay.events.flow.PaymentFlowV1"""

    envelope: str
    flow_id: str
    customer_id: str
    flow_type: str
    flow_status: str
    source_amount: Optional[str] = None
    target_amount: Optional[str] = None
    fx_rate: Optional[float] = None
    legs: list
    description: Optional[str] = None
