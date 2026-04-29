from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterator, Protocol, runtime_checkable


@dataclass(frozen=True)
class PaymentEvent:
    flow_type: str
    topic: str
    key: str
    schema_name: str
    record: dict[str, Any]


@runtime_checkable
class EventSource(Protocol):
    def events(self) -> Iterator[PaymentEvent]: ...
