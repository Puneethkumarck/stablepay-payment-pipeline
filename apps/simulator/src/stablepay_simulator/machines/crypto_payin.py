from __future__ import annotations

import uuid
from typing import Iterator

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

TOPIC = "payment.payin.crypto.v1"
SCHEMA = "PayinCryptoV1"

SCENARIOS: list[Scenario] = [
    Scenario(
        name="happy_path",
        weight=0.60,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=5.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=30.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=5.0),
            ScenarioStep(status="ALLOCATED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="stuck",
        weight=0.15,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=5.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=30.0),
            ScenarioStep(status="STUCK", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="screening_flag",
        weight=0.15,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=5.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=30.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_FLAGGED", delay_seconds=5.0),
            ScenarioStep(status="FAILED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="low_confirmations",
        weight=0.10,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=5.0),
            ScenarioStep(status="CONFIRMING", delay_seconds=60.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=5.0),
            ScenarioStep(status="ALLOCATED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
]

CHAINS = ["ETH", "BTC", "SOL"]
ASSETS = {"ETH": ["USDC", "USDT", "ETH"], "BTC": ["BTC"], "SOL": ["USDC", "SOL"]}


class CryptoPayinMachine(StateMachine):
    def run(self) -> Iterator[PaymentEvent]:
        scenario = select_scenario(SCENARIOS, self._rng)
        payin_ref = str(uuid.uuid4())
        customer_id = f"cust-{self._rng.randint(1, self._config.customer_pool_size):04d}"
        account_id = f"acc-{self._rng.randint(1, 500):04d}"
        chain = self._rng.choice(CHAINS)
        asset = self._rng.choice(ASSETS[chain])
        correlation_id = str(uuid.uuid4())
        amount_micros = self._rng.randint(10_000_000, 5_000_000_000)
        tx_hash = f"0x{self._rng.randbytes(32).hex()}"
        dest_address = f"0x{self._rng.randbytes(20).hex()}"
        source_address = f"0x{self._rng.randbytes(20).hex()}"
        confirmations = 0
        block_number = self._rng.randint(18_000_000, 22_000_000)

        for step in scenario.steps:
            if step.status in ("CONFIRMING", "CONFIRMED", "COMPLETED"):
                confirmations = min(confirmations + self._rng.randint(1, 5), 64)
                block_number += self._rng.randint(1, 3)

            record = {
                "envelope": make_envelope(correlation_id=correlation_id),
                "payin_reference": payin_ref,
                "customer_id": customer_id,
                "account_id": account_id,
                "amount": make_money(amount_micros, asset),
                "internal_status": step.status,
                "customer_status": derive_customer_status(step.status),
                "chain": chain,
                "asset": asset,
                "source_address": source_address,
                "destination_address": dest_address,
                "tx_hash": tx_hash,
                "confirmations": confirmations,
                "gas_fee_micros": self._rng.randint(500_000, 50_000_000),
                "block_number": block_number,
                "block_timestamp": None,
                "is_user_facing": True,
            }

            yield PaymentEvent(
                flow_type="crypto_payin",
                topic=TOPIC,
                key=payin_ref,
                schema_name=SCHEMA,
                record=record,
            )
