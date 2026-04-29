from __future__ import annotations

import uuid
from typing import Iterator

from faker import Faker

from stablepay_simulator.machines.auxiliary import emit_approval_events, emit_screening_events
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

TOPIC = "payment.payout.fiat.v1"
SCHEMA = "PayoutFiatV1"

SCENARIOS: list[Scenario] = [
    Scenario(
        name="happy_path",
        weight=0.60,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=5.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=10.0),
            ScenarioStep(status="SCREENING_RELEASED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_PARTNER_ROUTING", delay_seconds=2.0),
            ScenarioStep(status="PARTNER_ASSIGNED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_EXECUTION", delay_seconds=2.0),
            ScenarioStep(status="EXECUTING", delay_seconds=5.0),
            ScenarioStep(status="SENT_TO_PARTNER", delay_seconds=10.0),
            ScenarioStep(status="PARTNER_ACKNOWLEDGED", delay_seconds=5.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=15.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="screening_rejection",
        weight=0.10,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_REJECTED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="dual_approval_timeout",
        weight=0.05,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=5.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="PENDING_SECOND_APPROVAL", delay_seconds=30.0),
            ScenarioStep(status="EXPIRED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="partner_failure",
        weight=0.08,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_RELEASED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_PARTNER_ROUTING", delay_seconds=2.0),
            ScenarioStep(status="PARTNER_ASSIGNED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_EXECUTION", delay_seconds=2.0),
            ScenarioStep(status="EXECUTING", delay_seconds=5.0),
            ScenarioStep(status="FAILED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="refund_flow",
        weight=0.05,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_RELEASED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_PARTNER_ROUTING", delay_seconds=2.0),
            ScenarioStep(status="PARTNER_ASSIGNED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_EXECUTION", delay_seconds=2.0),
            ScenarioStep(status="EXECUTING", delay_seconds=5.0),
            ScenarioStep(status="SENT_TO_PARTNER", delay_seconds=10.0),
            ScenarioStep(status="RETURNED", delay_seconds=5.0),
            ScenarioStep(status="REFUND_INITIATED", delay_seconds=2.0),
            ScenarioStep(status="REFUND_PENDING", delay_seconds=10.0),
            ScenarioStep(status="REFUND_COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="confiscation",
        weight=0.02,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_SEIZED", delay_seconds=1.0),
            ScenarioStep(status="CONFISCATED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="manual_review",
        weight=0.05,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_HOLD", delay_seconds=5.0),
            ScenarioStep(status="SCREENING_RFI", delay_seconds=20.0),
            ScenarioStep(status="MANUAL_REVIEW", delay_seconds=30.0),
            ScenarioStep(status="SCREENING_RELEASED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_PARTNER_ROUTING", delay_seconds=2.0),
            ScenarioStep(status="PARTNER_ASSIGNED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_EXECUTION", delay_seconds=2.0),
            ScenarioStep(status="EXECUTING", delay_seconds=5.0),
            ScenarioStep(status="SENT_TO_PARTNER", delay_seconds=10.0),
            ScenarioStep(status="PARTNER_ACKNOWLEDGED", delay_seconds=5.0),
            ScenarioStep(status="PENDING_CONFIRMATION", delay_seconds=15.0),
            ScenarioStep(status="CONFIRMED", delay_seconds=2.0),
            ScenarioStep(status="COMPLETED", delay_seconds=0.0),
        ],
    ),
    Scenario(
        name="ledger_suspense",
        weight=0.05,
        steps=[
            ScenarioStep(status="INITIATED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_APPROVAL", delay_seconds=3.0),
            ScenarioStep(status="FIRST_APPROVAL", delay_seconds=2.0),
            ScenarioStep(status="APPROVED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_SCREENING", delay_seconds=2.0),
            ScenarioStep(status="SCREENING_IN_PROGRESS", delay_seconds=8.0),
            ScenarioStep(status="SCREENING_RELEASED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_PARTNER_ROUTING", delay_seconds=2.0),
            ScenarioStep(status="PARTNER_ASSIGNED", delay_seconds=1.0),
            ScenarioStep(status="PENDING_EXECUTION", delay_seconds=2.0),
            ScenarioStep(status="EXECUTING", delay_seconds=5.0),
            ScenarioStep(status="SENT_TO_PARTNER", delay_seconds=10.0),
            ScenarioStep(status="PARTNER_ACKNOWLEDGED", delay_seconds=5.0),
            ScenarioStep(status="LEDGER_SUSPENSE", delay_seconds=20.0),
            ScenarioStep(status="SUSPENDED", delay_seconds=0.0),
        ],
    ),
]

PROVIDERS = ["swift", "sepa", "ach", "wire", "fps"]
ROUTES = ["direct", "correspondent", "pooled"]
CURRENCIES = ["USD", "EUR", "GBP", "CHF", "SGD"]


class FiatPayoutMachine(StateMachine):
    def run(self) -> Iterator[PaymentEvent]:
        fake = Faker()
        fake.seed_instance(self._rng.randint(0, 2**32 - 1))

        scenario = select_scenario(SCENARIOS, self._rng)
        payout_ref = str(uuid.uuid4())
        customer_id = f"cust-{self._rng.randint(1, self._config.customer_pool_size):04d}"
        account_id = f"acc-{self._rng.randint(1, 500):04d}"
        currency = self._rng.choice(CURRENCIES)
        amount_micros = self._rng.randint(100_000_000, 10_000_000_000)
        correlation_id = str(uuid.uuid4())

        beneficiary = {
            "party_id": str(uuid.uuid4()),
            "name": fake.name(),
            "account_id": None,
            "iban": fake.iban(),
            "wallet_address": None,
            "address": None,
        }

        for step in scenario.steps:
            status = step.status
            if step.branches:
                roll = self._rng.random()
                cumulative = 0.0
                for branch in step.branches:
                    cumulative += branch.probability
                    if roll <= cumulative:
                        status = branch.status
                        break

            record = {
                "envelope": make_envelope(correlation_id=correlation_id),
                "payout_reference": payout_ref,
                "customer_id": customer_id,
                "account_id": account_id,
                "amount": make_money(amount_micros, currency),
                "fee": make_money(self._rng.randint(100_000, 5_000_000), currency),
                "source_amount": None,
                "target_amount": None,
                "fx_rate": None,
                "internal_status": status,
                "customer_status": derive_customer_status(status),
                "beneficiary": beneficiary,
                "provider": self._rng.choice(PROVIDERS),
                "route": self._rng.choice(ROUTES),
                "description": f"Payout {payout_ref[:8]}",
                "notes": None,
                "is_user_facing": True,
            }

            yield PaymentEvent(
                flow_type="fiat_payout",
                topic=TOPIC,
                key=payout_ref,
                schema_name=SCHEMA,
                record=record,
            )

            if status == "FIRST_APPROVAL":
                yield from emit_approval_events(
                    transaction_reference=payout_ref,
                    correlation_id=correlation_id,
                    decision="APPROVED",
                    approval_level=1,
                    rng=self._rng,
                )
            elif status == "SCREENING_RELEASED":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="CLEARED",
                    rng=self._rng,
                )
            elif status == "SCREENING_REJECTED":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="REJECTED",
                    rng=self._rng,
                )
            elif status == "SCREENING_SEIZED":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="SEIZED",
                    rng=self._rng,
                )
            elif status == "SCREENING_HOLD":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="HOLD",
                    rng=self._rng,
                )
            elif status == "SCREENING_RFI":
                yield from emit_screening_events(
                    transaction_reference=payout_ref,
                    customer_id=customer_id,
                    correlation_id=correlation_id,
                    outcome="RFI",
                    rng=self._rng,
                )
