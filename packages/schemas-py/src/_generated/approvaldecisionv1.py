"""Auto-generated from approval/approval_decision.avsc. Do not edit."""

from __future__ import annotations
from dataclasses import dataclass
from typing import Optional


@dataclass
class ApprovalDecisionV1:
    """io.stablepay.events.approval.ApprovalDecisionV1"""

    envelope: str
    approval_id: str
    transaction_reference: str
    approver_id: str
    decision: str
    approval_level: int
    notes: Optional[str] = None
