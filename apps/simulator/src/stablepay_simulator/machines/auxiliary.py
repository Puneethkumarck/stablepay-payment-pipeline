"""Auxiliary event generators for signing, screening, approval, and chain transactions.

These emit correlated events to their respective topics when a parent state machine
transitions through relevant statuses.
"""

from __future__ import annotations

import random
import uuid

from stablepay_simulator.machines.base import make_envelope, make_money
from stablepay_simulator.sources.protocol import PaymentEvent


def emit_screening_events(
    *,
    transaction_reference: str,
    customer_id: str,
    correlation_id: str,
    outcome: str,
    rng: random.Random,
) -> list[PaymentEvent]:
    screening_id = str(uuid.uuid4())
    providers = ["chainalysis", "elliptic", "refinitiv", "dow-jones"]

    record = {
        "envelope": make_envelope(correlation_id=correlation_id),
        "screening_id": screening_id,
        "transaction_reference": transaction_reference,
        "customer_id": customer_id,
        "outcome": outcome,
        "provider": rng.choice(providers),
        "risk_score": round(rng.uniform(0.0, 100.0), 2),
        "notes": None,
    }

    return [
        PaymentEvent(
            flow_type="screening",
            topic="screening.result.v1",
            key=transaction_reference,
            schema_name="ScreeningResultV1",
            record=record,
        )
    ]


def emit_approval_events(
    *,
    transaction_reference: str,
    correlation_id: str,
    decision: str,
    approval_level: int,
    rng: random.Random,
) -> list[PaymentEvent]:
    approval_id = str(uuid.uuid4())
    approver_id = f"approver-{rng.randint(1, 20):03d}"

    record = {
        "envelope": make_envelope(correlation_id=correlation_id),
        "approval_id": approval_id,
        "transaction_reference": transaction_reference,
        "approver_id": approver_id,
        "decision": decision,
        "approval_level": approval_level,
        "notes": None,
    }

    return [
        PaymentEvent(
            flow_type="approval",
            topic="approval.decision.v1",
            key=transaction_reference,
            schema_name="ApprovalDecisionV1",
            record=record,
        )
    ]


def emit_signing_events(
    *,
    transaction_reference: str,
    correlation_id: str,
    chain: str,
    statuses: list[str],
    rng: random.Random,
) -> list[PaymentEvent]:
    request_id = str(uuid.uuid4())
    raw_tx_hex = rng.randbytes(128).hex()
    events: list[PaymentEvent] = []

    for status in statuses:
        record = {
            "envelope": make_envelope(correlation_id=correlation_id),
            "request_id": request_id,
            "transaction_reference": transaction_reference,
            "chain": chain,
            "raw_transaction_hex": raw_tx_hex,
            "status": status,
        }

        events.append(
            PaymentEvent(
                flow_type="signing",
                topic="signing.request.v1",
                key=transaction_reference,
                schema_name="SigningRequestV1",
                record=record,
            )
        )

    return events


def emit_chain_transaction_events(
    *,
    tx_hash: str,
    chain: str,
    asset: str,
    from_address: str,
    to_address: str,
    amount_micros: int,
    correlation_id: str,
    statuses: list[str],
    rng: random.Random,
) -> list[PaymentEvent]:
    events: list[PaymentEvent] = []
    confirmations = 0
    block_number: int | None = None
    nonce = rng.randint(0, 999_999)

    for status in statuses:
        if status in ("CONFIRMING", "CONFIRMED"):
            confirmations += rng.randint(1, 5)
            if block_number is None:
                block_number = rng.randint(18_000_000, 22_000_000)
            else:
                block_number += rng.randint(1, 3)

        record = {
            "envelope": make_envelope(correlation_id=correlation_id),
            "tx_hash": tx_hash,
            "chain": chain,
            "asset": asset,
            "from_address": from_address,
            "to_address": to_address,
            "amount": make_money(amount_micros, asset),
            "gas_fee_micros": rng.randint(500_000, 50_000_000),
            "block_number": block_number,
            "block_timestamp": None,
            "confirmations": confirmations,
            "nonce": nonce,
            "is_replacement": False,
            "status": status,
        }

        events.append(
            PaymentEvent(
                flow_type="chain_transaction",
                topic="chain.transaction.v1",
                key=tx_hash,
                schema_name="ChainTransactionV1",
                record=record,
            )
        )

    return events
