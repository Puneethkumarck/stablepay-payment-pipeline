"""Tests for stablepay_dlq_tools.replayer."""

from __future__ import annotations

from typing import Any

import pytest

from stablepay_dlq_tools import replayer


class FakeProducer:
    def __init__(self) -> None:
        self.produced: list[dict[str, Any]] = []

    def produce(self, topic: str, *, value: bytes, headers: list[tuple[str, bytes]], on_delivery=None) -> None:
        self.produced.append({"topic": topic, "value": value, "headers": dict(headers)})
        if on_delivery is not None:
            on_delivery(None, _FakeMessage(topic))

    def poll(self, _timeout: float) -> None:
        return None

    def flush(self, *, timeout: float = 0.0) -> None:
        return None


class _FakeMessage:
    def __init__(self, topic: str) -> None:
        self._topic = topic

    def topic(self) -> str:
        return self._topic


def _event(payload: str = '{"k":"v"}', replay_count: int = 0) -> dict[str, Any]:
    return {
        "source_topic": "payment.flow.v1",
        "original_payload_json": payload,
        "replay_count": replay_count,
    }


def test_replay_event_increments_replay_count_header_from_zero() -> None:
    fake = FakeProducer()
    ok = replayer.replay_event(_event(replay_count=0), producer=fake)

    assert ok is True
    assert len(fake.produced) == 1
    assert fake.produced[0]["headers"] == {"replay_count": b"1"}
    assert fake.produced[0]["topic"] == "payment.flow.v1"


def test_replay_event_increments_existing_replay_count() -> None:
    fake = FakeProducer()
    replayer.replay_event(_event(replay_count=4), producer=fake)
    assert fake.produced[0]["headers"] == {"replay_count": b"5"}


def test_replay_event_handles_missing_replay_count_header() -> None:
    fake = FakeProducer()
    event = _event()
    event.pop("replay_count")
    replayer.replay_event(event, producer=fake)
    assert fake.produced[0]["headers"] == {"replay_count": b"1"}


def test_replay_event_returns_false_when_payload_missing() -> None:
    fake = FakeProducer()
    event = {"source_topic": "t", "replay_count": 0}
    ok = replayer.replay_event(event, producer=fake)
    assert ok is False
    assert fake.produced == []


def test_strip_nulls_transform_removes_null_fields() -> None:
    out = replayer.strip_nulls_transform({"original_payload_json": '{"a":1,"b":null}'})
    assert out == b'{"a": 1}'


def test_replay_batch_counts_skipped_events() -> None:
    events = [
        _event(replay_count=0),
        {"source_topic": "t"},  # missing payload
        {"original_payload_json": "{}"},  # missing source_topic
    ]

    fake = FakeProducer()

    def fake_build_producer() -> FakeProducer:  # type: ignore[override]
        return fake

    monkeypatched = pytest.MonkeyPatch()
    monkeypatched.setattr(replayer, "_build_producer", fake_build_producer)
    try:
        success, failed = replayer.replay_batch(events)
    finally:
        monkeypatched.undo()

    assert success == 1
    assert failed == 2
    assert len(fake.produced) == 1
    assert fake.produced[0]["headers"] == {"replay_count": b"1"}
