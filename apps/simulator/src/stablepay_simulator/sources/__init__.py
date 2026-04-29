from stablepay_simulator.sources.external_kafka_source import ExternalKafkaSource
from stablepay_simulator.sources.faker_source import FakerSource
from stablepay_simulator.sources.protocol import EventSource, PaymentEvent
from stablepay_simulator.sources.replay_source import ReplaySource

__all__ = [
    "EventSource",
    "ExternalKafkaSource",
    "FakerSource",
    "PaymentEvent",
    "ReplaySource",
    "get_source",
]


def get_source(source_type: str, **kwargs) -> EventSource:  # type: ignore[return]
    match source_type:
        case "faker":
            return FakerSource(**kwargs)
        case "replay":
            return ReplaySource()
        case "external_kafka":
            return ExternalKafkaSource()
        case _:
            msg = f"Unknown source type: {source_type}. Valid: faker, replay, external_kafka"
            raise ValueError(msg)
