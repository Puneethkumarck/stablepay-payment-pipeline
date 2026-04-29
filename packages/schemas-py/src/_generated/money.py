"""Auto-generated from common/money.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class Money:
    """io.stablepay.events.common.Money"""

    amount_micros: int
    currency_code: str
