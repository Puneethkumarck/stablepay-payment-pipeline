from __future__ import annotations

from typing import Iterator

from stablepay_simulator.sources.protocol import EventSource, PaymentEvent


class ExternalKafkaSource:
    """Consumes events from an external Kafka cluster. V2 feature."""

    def events(self) -> Iterator[PaymentEvent]:
        raise NotImplementedError("ExternalKafkaSource is a v2 feature")
