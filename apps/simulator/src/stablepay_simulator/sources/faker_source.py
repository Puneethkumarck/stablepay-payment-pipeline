from __future__ import annotations

import random
import time
from typing import TYPE_CHECKING, Iterator

import structlog

from stablepay_simulator.sources.protocol import EventSource, PaymentEvent

if TYPE_CHECKING:
    from stablepay_simulator.config import SimulatorConfig

logger = structlog.get_logger()

FLOW_TYPE_WEIGHTS: list[tuple[str, float]] = [
    ("fiat_payout", 0.30),
    ("crypto_payout", 0.20),
    ("fiat_payin", 0.20),
    ("crypto_payin", 0.15),
    ("multi_leg_flow", 0.15),
]


class FakerSource:
    def __init__(self, config: SimulatorConfig, rng: random.Random) -> None:
        self._config = config
        self._rng = rng
        self._flow_types = [ft for ft, _ in FLOW_TYPE_WEIGHTS]
        self._flow_weights = [w for _, w in FLOW_TYPE_WEIGHTS]

    def events(self) -> Iterator[PaymentEvent]:
        from stablepay_simulator.machines.base import select_machine

        while True:
            flow_type = self._rng.choices(self._flow_types, weights=self._flow_weights, k=1)[0]
            machine = select_machine(flow_type, self._config, self._rng)
            yield from machine.run()

            if not self._config.burst_active:
                delay = 1.0 / self._config.rate * self._config.delay_multiplier
                if delay > 0:
                    time.sleep(delay)
