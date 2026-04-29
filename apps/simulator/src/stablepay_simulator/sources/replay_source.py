from __future__ import annotations

from typing import Iterator

from stablepay_simulator.sources.protocol import EventSource, PaymentEvent


class ReplaySource:
    """Replays events from Iceberg raw tables. Available in Phase 3+."""

    def events(self) -> Iterator[PaymentEvent]:
        raise NotImplementedError("ReplaySource requires Iceberg raw tables (Phase 3+)")
