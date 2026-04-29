from __future__ import annotations

import random
import time
import uuid
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Iterator

from stablepay_simulator.sources.protocol import PaymentEvent

if TYPE_CHECKING:
    from stablepay_simulator.config import SimulatorConfig


@dataclass(frozen=True)
class Branch:
    status: str
    probability: float
    delay_seconds: float = 0.5


@dataclass(frozen=True)
class ScenarioStep:
    status: str
    delay_seconds: float = 1.0
    branches: list[Branch] = field(default_factory=list)


@dataclass(frozen=True)
class Scenario:
    name: str
    weight: float
    steps: list[ScenarioStep]


def select_scenario(scenarios: list[Scenario], rng: random.Random) -> Scenario:
    weights = [s.weight for s in scenarios]
    return rng.choices(scenarios, weights=weights, k=1)[0]


def make_envelope(
    *,
    flow_id: str | None = None,
    correlation_id: str | None = None,
    trace_id: str | None = None,
) -> dict:
    now_ms = int(time.time() * 1000)
    return {
        "event_id": str(uuid.uuid4()),
        "event_time": now_ms,
        "ingest_time": now_ms,
        "schema_version": "1.0.0",
        "flow_id": flow_id,
        "correlation_id": correlation_id,
        "trace_id": trace_id,
    }


def make_money(amount_micros: int, currency_code: str = "USD") -> dict:
    return {"amount_micros": amount_micros, "currency_code": currency_code}


CUSTOMER_STATUS_MAP: dict[str, str] = {
    "INITIATED": "PENDING",
    "PENDING_APPROVAL": "PENDING",
    "FIRST_APPROVAL": "PENDING",
    "PENDING_SECOND_APPROVAL": "PENDING",
    "APPROVED": "PROCESSING",
    "PENDING_SCREENING": "PROCESSING",
    "SCREENING_IN_PROGRESS": "PROCESSING",
    "SCREENING_HOLD": "PROCESSING",
    "SCREENING_RFI": "PROCESSING",
    "SCREENING_RELEASED": "PROCESSING",
    "SCREENING_REJECTED": "FAILED",
    "SCREENING_SEIZED": "FAILED",
    "SCREENING_CLEARED": "PROCESSING",
    "SCREENING_FLAGGED": "PROCESSING",
    "SCREENING_HELD": "PROCESSING",
    "PENDING_PARTNER_ROUTING": "PROCESSING",
    "PARTNER_ASSIGNED": "PROCESSING",
    "PENDING_EXECUTION": "PROCESSING",
    "EXECUTING": "PROCESSING",
    "SENT_TO_PARTNER": "PROCESSING",
    "PARTNER_ACKNOWLEDGED": "PROCESSING",
    "PENDING_CONFIRMATION": "PROCESSING",
    "CONFIRMED": "PROCESSING",
    "COMPLETED": "COMPLETED",
    "FAILED": "FAILED",
    "CANCELLED": "CANCELLED",
    "RETURNED": "REFUNDED",
    "REFUND_INITIATED": "PROCESSING",
    "REFUND_PENDING": "PROCESSING",
    "REFUND_COMPLETED": "REFUNDED",
    "CONFISCATED": "FAILED",
    "SUSPENDED": "PROCESSING",
    "LEDGER_SUSPENSE": "PROCESSING",
    "MANUAL_REVIEW": "PROCESSING",
    "EXPIRED": "FAILED",
    "PENDING_SIGNING": "PROCESSING",
    "SIGNING_IN_PROGRESS": "PROCESSING",
    "SIGNED": "PROCESSING",
    "BROADCASTING": "PROCESSING",
    "BROADCAST": "PROCESSING",
    "CONFIRMING": "PROCESSING",
    "STUCK": "PROCESSING",
    "RBF_INITIATED": "PROCESSING",
    "RBF_BROADCAST": "PROCESSING",
    "REPLACED": "PROCESSING",
    "DETECTED": "PENDING",
    "MATCHED": "PROCESSING",
    "PENDING_ALLOCATION": "PROCESSING",
    "ALLOCATED": "PROCESSING",
}


def derive_customer_status(internal_status: str) -> str:
    return CUSTOMER_STATUS_MAP.get(internal_status, "PROCESSING")


class StateMachine(ABC):
    def __init__(self, config: SimulatorConfig, rng: random.Random) -> None:
        self._config = config
        self._rng = rng

    @abstractmethod
    def run(self) -> Iterator[PaymentEvent]: ...


def select_machine(flow_type: str, config: SimulatorConfig, rng: random.Random) -> StateMachine:
    from stablepay_simulator.machines.crypto_payin import CryptoPayinMachine
    from stablepay_simulator.machines.crypto_payout import CryptoPayoutMachine
    from stablepay_simulator.machines.fiat_payin import FiatPayinMachine
    from stablepay_simulator.machines.fiat_payout import FiatPayoutMachine
    from stablepay_simulator.machines.multi_leg_flow import MultiLegFlowMachine

    machines: dict[str, type[StateMachine]] = {
        "fiat_payout": FiatPayoutMachine,
        "crypto_payout": CryptoPayoutMachine,
        "fiat_payin": FiatPayinMachine,
        "crypto_payin": CryptoPayinMachine,
        "multi_leg_flow": MultiLegFlowMachine,
    }
    cls = machines[flow_type]
    return cls(config, rng)
