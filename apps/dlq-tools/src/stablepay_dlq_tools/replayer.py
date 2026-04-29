"""Replays DLQ events back to their original Kafka topics."""

from __future__ import annotations

import json
from typing import Any, Callable

from confluent_kafka import Producer


TransformFn = Callable[[dict[str, Any]], bytes | None]


def _producer() -> Producer:
    import os

    return Producer(
        {
            "bootstrap.servers": os.getenv("STBLPAY_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
            "client.id": "dlq-replayer",
        }
    )


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


def replay_event(
    event: dict[str, Any],
    *,
    transform: TransformFn = identity_transform,
    target_topic: str | None = None,
) -> bool:
    topic = target_topic or event.get("source_topic")
    if not topic:
        return False

    payload = transform(event)
    if payload is None:
        return False

    producer = _producer()
    producer.produce(topic, value=payload)
    producer.flush(timeout=10)
    return True


def replay_batch(
    events: list[dict[str, Any]],
    *,
    transform: TransformFn = identity_transform,
    target_topic: str | None = None,
) -> tuple[int, int]:
    producer = _producer()
    success = 0
    failed = 0

    for event in events:
        topic = target_topic or event.get("source_topic")
        if not topic:
            failed += 1
            continue

        payload = transform(event)
        if payload is None:
            failed += 1
            continue

        producer.produce(topic, value=payload)
        success += 1

    producer.flush(timeout=30)
    return success, failed
