from __future__ import annotations

import uuid
from typing import Iterator

from stablepay_simulator.machines.base import (
    Scenario,
    ScenarioStep,
    StateMachine,
    make_envelope,
    make_money,
    select_scenario,
)
from stablepay_simulator.sources.protocol import PaymentEvent

FLOW_TOPIC = "payment.flow.v1"
FLOW_SCHEMA = "PaymentFlowV1"

FLOW_TYPES = ["ONRAMP", "OFFRAMP", "CRYPTO_TO_CRYPTO"]

SCENARIOS: list[Scenario] = [
    Scenario(name="happy_3_leg", weight=0.60, steps=[ScenarioStep(status="INITIATED")]),
    Scenario(name="partial_failure_compensation", weight=0.15, steps=[ScenarioStep(status="INITIATED")]),
    Scenario(name="payin_failure", weight=0.15, steps=[ScenarioStep(status="INITIATED")]),
    Scenario(name="payout_failure", weight=0.10, steps=[ScenarioStep(status="INITIATED")]),
]

PAYIN_HAPPY = [
    "DETECTED",
    "PENDING_SCREENING",
    "SCREENING_IN_PROGRESS",
    "SCREENING_CLEARED",
    "MATCHED",
    "PENDING_ALLOCATION",
    "ALLOCATED",
    "COMPLETED",
]
PAYIN_FAIL = ["DETECTED", "PENDING_SCREENING", "SCREENING_IN_PROGRESS", "SCREENING_FLAGGED", "FAILED"]

PAYOUT_HAPPY = [
    "INITIATED",
    "PENDING_APPROVAL",
    "APPROVED",
    "PENDING_SCREENING",
    "SCREENING_IN_PROGRESS",
    "SCREENING_CLEARED",
    "PENDING_SIGNING",
    "SIGNING_IN_PROGRESS",
    "SIGNED",
    "BROADCASTING",
    "BROADCAST",
    "CONFIRMING",
    "CONFIRMED",
    "COMPLETED",
]
PAYOUT_FAIL = [
    "INITIATED",
    "PENDING_APPROVAL",
    "APPROVED",
    "PENDING_SCREENING",
    "SCREENING_IN_PROGRESS",
    "SCREENING_CLEARED",
    "PENDING_SIGNING",
    "SIGNING_IN_PROGRESS",
    "FAILED",
]

CURRENCIES_FIAT = ["USD", "EUR", "GBP"]
CURRENCIES_CRYPTO = ["USDC", "USDT", "ETH", "BTC"]


class MultiLegFlowMachine(StateMachine):
    def run(self) -> Iterator[PaymentEvent]:
        scenario = select_scenario(SCENARIOS, self._rng)
        flow_id = str(uuid.uuid4())
        customer_id = f"cust-{self._rng.randint(1, self._config.customer_pool_size):04d}"
        flow_type = self._rng.choice(FLOW_TYPES)

        source_currency = self._rng.choice(CURRENCIES_FIAT if flow_type == "ONRAMP" else CURRENCIES_CRYPTO)
        target_currency = self._rng.choice(CURRENCIES_CRYPTO if flow_type == "ONRAMP" else CURRENCIES_FIAT)
        if flow_type == "CRYPTO_TO_CRYPTO":
            source_currency = self._rng.choice(CURRENCIES_CRYPTO)
            target_currency = self._rng.choice([c for c in CURRENCIES_CRYPTO if c != source_currency])

        source_amount_micros = self._rng.randint(100_000_000, 10_000_000_000)
        target_amount_micros = self._rng.randint(100_000_000, 10_000_000_000)

        payin_ref = str(uuid.uuid4())
        payout_ref = str(uuid.uuid4())
        trade_ref = str(uuid.uuid4())

        legs = [
            {"leg_id": payin_ref, "leg_type": "PAYIN", "status": "PENDING", "reference": payin_ref},
            {"leg_id": trade_ref, "leg_type": "TRADE", "status": "PENDING", "reference": trade_ref},
            {"leg_id": payout_ref, "leg_type": "PAYOUT", "status": "PENDING", "reference": payout_ref},
        ]

        initiated_record = {
            "envelope": make_envelope(flow_id=flow_id),
            "flow_id": flow_id,
            "customer_id": customer_id,
            "flow_type": flow_type,
            "flow_status": "INITIATED",
            "source_amount": make_money(source_amount_micros, source_currency),
            "target_amount": make_money(target_amount_micros, target_currency),
            "fx_rate": None,
            "legs": legs,
            "description": f"Multi-leg {flow_type} flow {flow_id[:8]}",
        }

        yield PaymentEvent(
            flow_type="multi_leg_flow",
            topic=FLOW_TOPIC,
            key=flow_id,
            schema_name=FLOW_SCHEMA,
            record=initiated_record,
        )

        is_onramp = flow_type in ("ONRAMP",)
        payin_statuses = PAYIN_HAPPY if scenario.name != "payin_failure" else PAYIN_FAIL
        payout_statuses = (
            PAYOUT_HAPPY if scenario.name not in ("payout_failure", "partial_failure_compensation") else PAYOUT_FAIL
        )

        if is_onramp:
            payin_topic = "payment.payin.fiat.v1"
            payin_schema = "PayinFiatV1"
        else:
            payin_topic = "payment.payin.crypto.v1"
            payin_schema = "PayinCryptoV1"

        for status in payin_statuses:
            record = _build_payin_record(
                payin_ref=payin_ref,
                customer_id=customer_id,
                status=status,
                flow_id=flow_id,
                amount_micros=source_amount_micros,
                currency=source_currency,
                is_fiat=is_onramp,
                rng=self._rng,
            )
            yield PaymentEvent(
                flow_type="multi_leg_flow",
                topic=payin_topic,
                key=payin_ref,
                schema_name=payin_schema,
                record=record,
            )

        if scenario.name == "payin_failure":
            return

        is_crypto_payout = flow_type in ("ONRAMP", "CRYPTO_TO_CRYPTO")
        payout_topic = "payment.payout.crypto.v1" if is_crypto_payout else "payment.payout.fiat.v1"
        payout_schema = "PayoutCryptoV1" if is_crypto_payout else "PayoutFiatV1"

        for status in payout_statuses:
            record = _build_payout_record(
                payout_ref=payout_ref,
                customer_id=customer_id,
                status=status,
                flow_id=flow_id,
                amount_micros=target_amount_micros,
                currency=target_currency,
                rng=self._rng,
            )
            yield PaymentEvent(
                flow_type="multi_leg_flow",
                topic=payout_topic,
                key=payout_ref,
                schema_name=payout_schema,
                record=record,
            )


def _build_payin_record(
    *,
    payin_ref: str,
    customer_id: str,
    status: str,
    flow_id: str,
    amount_micros: int,
    currency: str,
    is_fiat: bool,
    rng: "random.Random",
) -> dict:
    from stablepay_simulator.machines.base import derive_customer_status

    base = {
        "envelope": make_envelope(flow_id=flow_id, correlation_id=str(uuid.uuid4())),
        "payin_reference": payin_ref,
        "customer_id": customer_id,
        "account_id": f"acc-{rng.randint(1, 500):04d}",
        "amount": make_money(amount_micros, currency),
        "internal_status": status,
        "customer_status": derive_customer_status(status),
        "is_user_facing": True,
    }

    if is_fiat:
        base["fee"] = make_money(rng.randint(50_000, 1_000_000), currency)
        base["sender"] = None
        base["receiver_account"] = base["account_id"]
        base["bank_reference"] = f"BANK-{rng.randbytes(6).hex().upper()}"
        base["description"] = f"Flow payin {payin_ref[:8]}"
    else:
        base["chain"] = rng.choice(["ETH", "SOL"])
        base["asset"] = currency
        base["source_address"] = f"0x{rng.randbytes(20).hex()}"
        base["destination_address"] = f"0x{rng.randbytes(20).hex()}"
        base["tx_hash"] = f"0x{rng.randbytes(32).hex()}"
        base["confirmations"] = rng.randint(1, 30)
        base["gas_fee_micros"] = rng.randint(500_000, 10_000_000)
        base["block_number"] = rng.randint(18_000_000, 22_000_000)
        base["block_timestamp"] = None

    return base


def _build_payout_record(
    *,
    payout_ref: str,
    customer_id: str,
    status: str,
    flow_id: str,
    amount_micros: int,
    currency: str,
    rng: "random.Random",
) -> dict:
    from stablepay_simulator.machines.base import derive_customer_status

    chain = rng.choice(["ETH", "BTC", "SOL"])
    tx_hash = (
        f"0x{rng.randbytes(32).hex()}"
        if status
        in (
            "BROADCAST",
            "CONFIRMING",
            "CONFIRMED",
            "COMPLETED",
            "RBF_BROADCAST",
            "REPLACED",
        )
        else None
    )

    return {
        "envelope": make_envelope(flow_id=flow_id, correlation_id=str(uuid.uuid4())),
        "payout_reference": payout_ref,
        "customer_id": customer_id,
        "account_id": f"acc-{rng.randint(1, 500):04d}",
        "amount": make_money(amount_micros, currency),
        "fee": make_money(rng.randint(10_000, 2_000_000), currency),
        "internal_status": status,
        "customer_status": derive_customer_status(status),
        "chain": chain,
        "asset": currency,
        "source_address": f"0x{rng.randbytes(20).hex()}",
        "destination_address": f"0x{rng.randbytes(20).hex()}",
        "tx_hash": tx_hash,
        "confirmations": rng.randint(1, 30) if tx_hash else 0,
        "gas_fee_micros": rng.randint(500_000, 50_000_000) if tx_hash else None,
        "block_number": rng.randint(18_000_000, 22_000_000) if tx_hash else None,
        "block_timestamp": None,
        "description": f"Flow payout {payout_ref[:8]}",
        "is_user_facing": True,
    }
