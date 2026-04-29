"""Auto-generated from common/party.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class Party:
    """io.stablepay.events.common.Party"""

    party_id: Optional[str] = None
    name: Optional[str] = None
    account_id: Optional[str] = None
    iban: Optional[str] = None
    wallet_address: Optional[str] = None
    address: Optional[str] = None
