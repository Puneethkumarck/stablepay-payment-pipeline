"""Replays DLQ events back to their original Kafka topics."""

from __future__ import annotations

import json
import os
from typing import Any, Callable

from confluent_kafka import KafkaError, Message, Producer

TransformFn = Callable[[dict[str, Any]], bytes | None]


def _producer_config() -> dict[str, Any]:
    return {
        "bootstrap.servers": os.getenv("STBLPAY_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        "client.id": "dlq-replayer",
        "acks": "all",
        "enable.idempotence": True,
        "retries": 10,
        "retry.backoff.ms": 100,
    }


def _build_producer() -> Producer:
    return Producer(_producer_config())


def _next_replay_count(event: dict[str, Any]) -> int:
    raw = event.get("replay_count", 0)
    try:
        return int(raw) + 1
    except (TypeError, ValueError):
        return 1


def identity_transform(event: dict[str, Any]) -> bytes | None:
    payload = event.get("original_payload_json")
    if payload is None:
        return None
    return payload.encode("utf-8") if isinstance(payload, str) else payload


def strip_nulls_transform(event: dict[str, Any]) -> bytes | None:
    payload = event.get("original_payload_json")
    if payload is None:
        return None
    parsed = json.loads(payload) if isinstance(payload, str) else payload
    cleaned = {k: v for k, v in parsed.items() if v is not None}
    return json.dumps(cleaned).encode("utf-8")


TRANSFORMS: dict[str, TransformFn] = {
    "identity": identity_transform,
    "strip-nulls": strip_nulls_transform,
}


class _DeliveryTracker:
    def __init__(self) -> None:
        self.failures = 0

    def __call__(self, err: KafkaError | None, _msg: Message) -> None:
        if err is not None:
            self.failures += 1


def _produce(
    producer: Producer,
    *,
    topic: str,
    payload: bytes,
    replay_count: int,
    callback: Callable[[KafkaError | None, Message], None],
) -> None:
    headers = [("replay_count", str(replay_count).encode("utf-8"))]
    producer.produce(topic, value=payload, headers=headers, on_delivery=callback)
    producer.poll(0)


def replay_event(
    event: dict[str, Any],
    *,
    transform: TransformFn = identity_transform,
    target_topic: str | None = None,
    producer: Producer | None = None,
) -> bool:
    topic = target_topic or event.get("source_topic")
    if not topic:
        return False

    payload = transform(event)
    if payload is None:
        return False

    owns_producer = producer is None
    p = producer if producer is not None else _build_producer()
    tracker = _DeliveryTracker()

    _produce(p, topic=topic, payload=payload, replay_count=_next_replay_count(event), callback=tracker)

    if owns_producer:
        p.flush(timeout=10)

    return tracker.failures == 0


def replay_batch(
    events: list[dict[str, Any]],
    *,
    transform: TransformFn = identity_transform,
    target_topic: str | None = None,
) -> tuple[int, int]:
    producer = _build_producer()
    tracker = _DeliveryTracker()
    enqueued = 0
    skipped = 0

    for event in events:
        topic = target_topic or event.get("source_topic")
        if not topic:
            skipped += 1
            continue

        payload = transform(event)
        if payload is None:
            skipped += 1
            continue

        _produce(producer, topic=topic, payload=payload, replay_count=_next_replay_count(event), callback=tracker)
        enqueued += 1

    producer.flush(timeout=30)
    success = enqueued - tracker.failures
    failed = skipped + tracker.failures
    return success, failed
