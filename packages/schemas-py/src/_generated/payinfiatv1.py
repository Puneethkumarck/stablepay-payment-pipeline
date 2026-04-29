"""Auto-generated from payin-fiat/payment_payin_fiat.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class PayinFiatV1:
    """io.stablepay.events.payments.PayinFiatV1"""

    envelope: str
    payin_reference: str
    customer_id: str
    account_id: str
    amount: str
    fee: Optional[str] = None
    internal_status: str
    customer_status: str
    sender: Optional[str] = None
    receiver_account: Optional[str] = None
    bank_reference: Optional[str] = None
    description: Optional[str] = None
    is_user_facing: bool
