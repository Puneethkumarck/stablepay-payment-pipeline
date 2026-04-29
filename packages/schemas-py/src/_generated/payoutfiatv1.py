"""Auto-generated from payout-fiat/payment_payout_fiat.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class PayoutFiatV1:
    """io.stablepay.events.payments.PayoutFiatV1"""

    envelope: str
    payout_reference: str
    customer_id: str
    account_id: str
    amount: str
    fee: Optional[str] = None
    source_amount: Optional[str] = None
    target_amount: Optional[str] = None
    fx_rate: Optional[float] = None
    internal_status: str
    customer_status: str
    beneficiary: Optional[str] = None
    provider: Optional[str] = None
    route: Optional[str] = None
    description: Optional[str] = None
    notes: Optional[str] = None
    is_user_facing: bool
