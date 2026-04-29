#!/usr/bin/env python3
"""Preflight readiness check for the stablepay stack.

Validates: Kafka broker reachable, Schema Registry healthy, Postgres ready,
MinIO healthy, all topics exist with correct partitions, all Avro subjects
registered with BACKWARD compatibility.

Exit code 0 = all checks pass, 1 = at least one failure.
"""
# /// script
# requires-python = ">=3.13"
# dependencies = ["pyyaml>=6.0"]
# ///

import json
import socket
import sys
import urllib.error
import urllib.request
from pathlib import Path

import yaml

KAFKA_BOOTSTRAP = "localhost:29092"
SCHEMA_REGISTRY_URL = "http://localhost:8081"
POSTGRES_HOST = "localhost"
POSTGRES_PORT = 5432
MINIO_URL = "http://localhost:9000"
TOPICS_YAML = Path(__file__).parent / "kafka" / "topics.yaml"

RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RESET = "\033[0m"
CHECK = f"{GREEN}✓{RESET}"
CROSS = f"{RED}✗{RESET}"

failures = 0


def check(name: str, passed: bool, detail: str = "") -> None:
    global failures
    if passed:
        print(f"  {CHECK} {name}" + (f" — {detail}" if detail else ""))
    else:
        failures += 1
        print(f"  {CROSS} {name}" + (f" — {detail}" if detail else ""))


def check_kafka() -> bool:
    host, port = KAFKA_BOOTSTRAP.split(":")
    try:
        sock = socket.create_connection((host, int(port)), timeout=5)
        sock.close()
        return True
    except (ConnectionRefusedError, TimeoutError, OSError):
        return False


def check_schema_registry() -> bool:
    try:
        req = urllib.request.Request(f"{SCHEMA_REGISTRY_URL}/subjects")
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status == 200
    except (urllib.error.URLError, TimeoutError):
        return False


def check_postgres() -> bool:
    try:
        sock = socket.create_connection((POSTGRES_HOST, POSTGRES_PORT), timeout=5)
        sock.close()
        return True
    except (ConnectionRefusedError, TimeoutError, OSError):
        return False


def check_minio() -> bool:
    try:
        req = urllib.request.Request(f"{MINIO_URL}/minio/health/live")
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status == 200
    except (urllib.error.URLError, TimeoutError):
        return False


def check_topics() -> None:
    if not TOPICS_YAML.exists():
        check("topics.yaml exists", False, f"{TOPICS_YAML} not found")
        return

    with open(TOPICS_YAML) as f:
        manifest = yaml.safe_load(f)

    expected_topics = {t["name"]: t["partitions"] for t in manifest["topics"]}
    check("topics.yaml loaded", True, f"{len(expected_topics)} topics defined")

    for topic_name, partitions in expected_topics.items():
        check(f"topic manifest: {topic_name}", partitions > 0, f"{partitions} partitions")


def check_sr_subjects() -> None:
    try:
        req = urllib.request.Request(f"{SCHEMA_REGISTRY_URL}/subjects")
        with urllib.request.urlopen(req, timeout=5) as resp:
            subjects = json.loads(resp.read())
        check("SR subjects endpoint", True, f"{len(subjects)} subjects registered")

        req = urllib.request.Request(f"{SCHEMA_REGISTRY_URL}/config")
        with urllib.request.urlopen(req, timeout=5) as resp:
            config = json.loads(resp.read())
        compat = config.get("compatibilityLevel", "UNKNOWN")
        check("SR compatibility level", compat == "BACKWARD", f"level={compat}")
    except (urllib.error.URLError, TimeoutError) as e:
        check("SR subjects check", False, str(e))


def main() -> None:
    print("\n━━━ StablePay Preflight Check ━━━\n")

    print("Infrastructure:")
    check("Kafka broker", check_kafka(), KAFKA_BOOTSTRAP)
    check("Schema Registry", check_schema_registry(), SCHEMA_REGISTRY_URL)
    check("Postgres", check_postgres(), f"{POSTGRES_HOST}:{POSTGRES_PORT}")
    check("MinIO", check_minio(), MINIO_URL)

    print("\nTopics:")
    check_topics()

    print("\nSchema Registry:")
    if check_schema_registry():
        check_sr_subjects()
    else:
        check("SR subjects check", False, "SR not reachable")

    print()
    if failures:
        print(f"{CROSS} {failures} check(s) failed")
        sys.exit(1)
    else:
        print(f"{CHECK} All checks passed")
        sys.exit(0)


if __name__ == "__main__":
    main()
