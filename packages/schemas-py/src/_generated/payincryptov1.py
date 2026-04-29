"""Auto-generated from payin-crypto/payment_payin_crypto.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class PayinCryptoV1:
    """io.stablepay.events.payments.PayinCryptoV1"""

    envelope: str
    payin_reference: str
    customer_id: str
    account_id: str
    amount: str
    internal_status: str
    customer_status: str
    chain: str
    asset: str
    source_address: Optional[str] = None
    destination_address: str
    tx_hash: Optional[str] = None
    confirmations: int
    gas_fee_micros: Optional[int] = None
    block_number: Optional[int] = None
    block_timestamp: Optional[int] = None
    is_user_facing: bool
