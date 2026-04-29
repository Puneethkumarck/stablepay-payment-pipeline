from __future__ import annotations

import uuid
from typing import Iterator

from faker import Faker

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

TOPIC = "payment.payin.fiat.v1"
SCHEMA = "PayinFiatV1"

SCENARIOS: list[Scenario] = [
    Scenario(
        name="happy_path",
        weight=0.65,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="MATCHED", delay_seconds=3.0),
            ScenarioStep(status="PENDING_ALLOCATION", delay_seconds=2.0),
            ScenarioStep(status="ALLOCATED", delay_seconds=1.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="screening_flag",
        weight=0.15,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_FLAGGED", delay_seconds=5.0),
            ScenarioStep(status="SCREENING_HELD", delay_seconds=20.0),
            ScenarioStep(status="SUSPENDED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="return",
        weight=0.10,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="MATCHED", delay_seconds=3.0),
            ScenarioStep(status="PENDING_ALLOCATION", delay_seconds=2.0),
            ScenarioStep(status="RETURNED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="allocation_failure",
        weight=0.10,
        steps=[
            ScenarioStep(status="DETECTED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_CLEARED", delay_seconds=1.0),
            ScenarioStep(status="MATCHED", delay_seconds=3.0),
            ScenarioStep(status="PENDING_ALLOCATION", delay_seconds=2.0),
            ScenarioStep(status="FAILED", delay_seconds=0.0),
        ],
    ),
]

CURRENCIES = ["USD", "EUR", "GBP", "CHF"]


class FiatPayinMachine(StateMachine):
    def run(self) -> Iterator[PaymentEvent]:
        fake = Faker()
        fake.seed_instance(self._rng.randint(0, 2**32 - 1))

        scenario = select_scenario(SCENARIOS, self._rng)
        payin_ref = str(uuid.uuid4())
        customer_id = f"cust-{self._rng.randint(1, self._config.customer_pool_size):04d}"
        account_id = f"acc-{self._rng.randint(1, 500):04d}"
        currency = self._rng.choice(CURRENCIES)
        amount_micros = self._rng.randint(50_000_000, 8_000_000_000)
        correlation_id = str(uuid.uuid4())
        bank_reference = f"BANK-{self._rng.randbytes(6).hex().upper()}"

        sender = {
            "party_id": str(uuid.uuid4()),
            "name": fake.name(),
            "account_id": None,
            "iban": fake.iban(),
            "wallet_address": None,
            "address": None,
        }

        for step in scenario.steps:
            record = {
                "envelope": make_envelope(correlation_id=correlation_id),
                "payin_reference": payin_ref,
                "customer_id": customer_id,
                "account_id": account_id,
                "amount": make_money(amount_micros, currency),
                "fee": make_money(self._rng.randint(50_000, 2_000_000), currency),
                "internal_status": step.status,
                "customer_status": derive_customer_status(step.status),
                "sender": sender,
                "receiver_account": account_id,
                "bank_reference": bank_reference,
                "description": f"Fiat payin {payin_ref[:8]}",
                "is_user_facing": True,
            }

            yield PaymentEvent(
                flow_type="fiat_payin",
                topic=TOPIC,
                key=payin_ref,
                schema_name=SCHEMA,
                record=record,
            )
