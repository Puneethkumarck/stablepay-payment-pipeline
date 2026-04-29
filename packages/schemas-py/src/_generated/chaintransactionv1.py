"""Auto-generated from chain/chain_transaction.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class ChainTransactionV1:
    """io.stablepay.events.chain.ChainTransactionV1"""

    envelope: str
    tx_hash: str
    chain: str
    asset: str
    from_address: str
    to_address: str
    amount: str
    gas_fee_micros: Optional[int] = None
    block_number: Optional[int] = None
    block_timestamp: Optional[int] = None
    confirmations: int
    nonce: Optional[int] = None
    is_replacement: bool
    status: str
