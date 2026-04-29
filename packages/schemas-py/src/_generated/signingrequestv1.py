"""Auto-generated from signing/signing_request.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class SigningRequestV1:
    """io.stablepay.events.signing.SigningRequestV1"""

    envelope: str
    request_id: str
    transaction_reference: str
    chain: str
    raw_transaction_hex: str
    status: str
