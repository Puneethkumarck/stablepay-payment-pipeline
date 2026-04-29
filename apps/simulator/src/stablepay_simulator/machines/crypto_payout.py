from __future__ import annotations

import uuid
from typing import Iterator

from stablepay_simulator.machines.auxiliary import (
    emit_chain_transaction_events,
    emit_screening_events,
    emit_signing_events,
)
from stablepay_simulator.machines.base import (
    Scenario,
    ScenarioStep,
    StateMachine,
    derive_customer_status,
    make_envelope,
    make_money,
    select_scenario,
)
from stablepay_simulator.sources.protocol import PaymentEvent

TOPIC = "payment.payout.crypto.v1"
SCHEMA = "PayoutCryptoV1"

SCENARIOS: list[Scenario] = [
    Scenario(
        name="happy_path",
        weight=0.55,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SIGNING", delay_seconds=2.0),
            ScenarioStep(status="SIGNING_IN_PROGRESS", delay_seconds=5.0),
            ScenarioStep(status="SIGNED", delay_seconds=1.0),
            ScenarioStep(status="BROADCASTING", delay_seconds=3.0),
            ScenarioStep(status="BROADCAST", delay_seconds=2.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=5.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=30.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="screening_flag",
        weight=0.10,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_FLAGGED", delay_seconds=5.0),
            ScenarioStep(status="CANCELLED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="signing_failure",
        weight=0.10,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SIGNING", delay_seconds=2.0),
            ScenarioStep(status="SIGNING_IN_PROGRESS", delay_seconds=5.0),
            ScenarioStep(status="FAILED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="stuck_rbf",
        weight=0.10,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SIGNING", delay_seconds=2.0),
            ScenarioStep(status="SIGNING_IN_PROGRESS", delay_seconds=5.0),
            ScenarioStep(status="SIGNED", delay_seconds=1.0),
            ScenarioStep(status="BROADCASTING", delay_seconds=3.0),
            ScenarioStep(status="BROADCAST", delay_seconds=2.0),
            ScenarioStep(status="STUCK", delay_seconds=60.0),
            ScenarioStep(status="RBF_INITIATED", delay_seconds=5.0),
            ScenarioStep(status="RBF_BROADCAST", delay_seconds=10.0),
            ScenarioStep(status="REPLACED", delay_seconds=2.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=30.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="broadcast_failure",
        weight=0.10,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SIGNING", delay_seconds=2.0),
            ScenarioStep(status="SIGNING_IN_PROGRESS", delay_seconds=5.0),
            ScenarioStep(status="SIGNED", delay_seconds=1.0),
            ScenarioStep(status="BROADCASTING", delay_seconds=3.0),
            ScenarioStep(status="FAILED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="confiscation",
        weight=0.05,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_FLAGGED", delay_seconds=5.0),
            ScenarioStep(status="CONFISCATED", delay_seconds=0.0),
        ],
    ),
]

CHAINS = ["ETH", "BTC", "SOL"]
ASSETS = {"ETH": ["USDC", "USDT", "ETH"], "BTC": ["BTC"], "SOL": ["USDC", "SOL"]}


class CryptoPayoutMachine(StateMachine):
    def run(self) -> Iterator[PaymentEvent]:
        scenario = select_scenario(SCENARIOS, self._rng)
        payout_ref = str(uuid.uuid4())
        customer_id = f"cust-{self._rng.randint(1, self._config.customer_pool_size):04d}"
        account_id = f"acc-{self._rng.randint(1, 500):04d}"
        chain = self._rng.choice(CHAINS)
        asset = self._rng.choice(ASSETS[chain])
        correlation_id = str(uuid.uuid4())
        amount_micros = self._rng.randint(10_000_000, 5_000_000_000)
        dest_address = f"0x{self._rng.randbytes(20).hex()}"
        source_address = f"0x{self._rng.randbytes(20).hex()}"
        tx_hash: str | None = None
        confirmations = 0
        block_number: int | None = None

        for step in scenario.steps:
            status = step.status

            if status in ("BROADCAST", "CONFIRMING", "CONFIRMED", "COMPLETED", "RBF_BROADCAST", "REPLACED"):
                if tx_hash is None:
                    tx_hash = f"0x{self._rng.randbytes(32).hex()}"
                confirmations = min(confirmations + self._rng.randint(1, 5), 64)
                if block_number is None:
                    block_number = self._rng.randint(18_000_000, 22_000_000)
                else:
                    block_number += self._rng.randint(1, 3)

            record = {
                "envelope": make_envelope(correlation_id=correlation_id),
                "payout_reference": payout_ref,
                "customer_id": customer_id,
                "account_id": account_id,
                "amount": make_money(amount_micros, asset),
                "fee": make_money(self._rng.randint(10_000, 2_000_000), asset),
                "internal_status": status,
                "customer_status": derive_customer_status(status),
                "chain": chain,
                "asset": asset,
                "source_address": source_address,
                "destination_address": dest_address,
                "tx_hash": tx_hash,
                "confirmations": confirmations,
                "gas_fee_micros": self._rng.randint(500_000, 50_000_000) if tx_hash else None,
                "block_number": block_number,
                "block_timestamp": None,
                "description": f"Crypto payout {payout_ref[:8]}",
                "is_user_facing": True,
            }

            yield PaymentEvent(
                flow_type="crypto_payout",
                topic=TOPIC,
                key=payout_ref,
                schema_name=SCHEMA,
                record=record,
            )

            if status == "SCREENING_CLEARED":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="CLEARED",
                    rng=self._rng,
                )
            elif status == "SCREENING_FLAGGED":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="FLAGGED",
                    rng=self._rng,
                )
            elif status == "SIGNED":
                yield from emit_signing_events(
                    transaction_reference=payout_ref,
                    correlation_id=correlation_id,
                    chain=chain,
                    statuses=["REQUESTED", "IN_PROGRESS", "SIGNED"],
                    rng=self._rng,
                )
            elif status == "BROADCAST":
                if tx_hash:
                    yield from emit_chain_transaction_events(
                        tx_hash=tx_hash,
                        chain=chain,
                        asset=asset,
                        from_address=source_address,
                        to_address=dest_address,
                        amount_micros=amount_micros,
                        correlation_id=correlation_id,
                        statuses=["PENDING", "BROADCAST"],
                        rng=self._rng,
                    )
            elif status == "CONFIRMED":
                if tx_hash:
                    yield from emit_chain_transaction_events(
                        tx_hash=tx_hash,
                        chain=chain,
                        asset=asset,
                        from_address=source_address,
                        to_address=dest_address,
                        amount_micros=amount_micros,
                        correlation_id=correlation_id,
                        statuses=["CONFIRMING", "CONFIRMED"],
                        rng=self._rng,
                    )
