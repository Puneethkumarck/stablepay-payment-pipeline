from __future__ import annotations

import os
import random
import signal
import sys
import threading
import time
from pathlib import Path

import click
import structlog

from stablepay_simulator.config import SimulatorConfig, load_config
from stablepay_simulator.producer import TransactionalAvroProducer
from stablepay_simulator.sources import get_source

structlog.configure(
    processors=[
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),
    ],
)
logger = structlog.get_logger()

_shutdown = threading.Event()


def _handle_signal(signum: int, _frame: object) -> None:
    logger.info("shutdown_requested", signal=signum)
    _shutdown.set()


@click.command()
@click.option("--rate", type=int, default=None, help="Events per second (default: 200)")
@click.option("--delay-multiplier", type=float, default=None, help="Delay multiplier (default: 0.01)")
@click.option("--burst", is_flag=True, default=False, help="Enable periodic burst mode")
@click.option("--burst-interval", type=int, default=None, help="Seconds between bursts (default: 600)")
@click.option("--realistic", is_flag=True, default=False, help="Set delay-multiplier=1.0 for realistic timing")
@click.option("--seed", type=int, default=None, help="Random seed for reproducibility")
@click.option("--source-type", type=str, default="faker", help="Event source: faker, replay, external_kafka")
@click.option("--config-dir", type=click.Path(exists=True, path_type=Path), default=None, help="Config directory")
def main(
    rate: int | None,
    delay_multiplier: float | None,
    burst: bool,
    burst_interval: int | None,
    realistic: bool,
    seed: int | None,
    source_type: str,
    config_dir: Path | None,
) -> None:
    """StablePay payment event simulator."""
    signal.signal(signal.SIGINT, _handle_signal)
    signal.signal(signal.SIGTERM, _handle_signal)

    if config_dir is None:
        config_dir = Path(__file__).resolve().parents[2] / "config"

    overrides: dict = {}
    if rate is not None:
        overrides["rate"] = rate
    if delay_multiplier is not None:
        overrides["delay_multiplier"] = delay_multiplier
    if realistic:
        overrides["delay_multiplier"] = 1.0
    if burst:
        overrides["burst_enabled"] = True
    if burst_interval is not None:
        overrides["burst_interval"] = burst_interval
    if seed is not None:
        overrides["seed"] = seed

    config = load_config(config_dir, overrides)
    rng = random.Random(config.seed)

    bootstrap_servers = os.environ.get("STBLPAY_KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
    schema_registry_url = os.environ.get("STBLPAY_SCHEMA_REGISTRY_URL", "http://localhost:8081")

    logger.info(
        "simulator_starting",
        rate=config.rate,
        delay_multiplier=config.delay_multiplier,
        burst=config.burst_enabled,
        source_type=source_type,
        bootstrap_servers=bootstrap_servers,
        schema_registry_url=schema_registry_url,
    )

    producer = TransactionalAvroProducer(
        bootstrap_servers=bootstrap_servers,
        schema_registry_url=schema_registry_url,
    )
    producer.init()

    source = get_source(source_type, config=config, rng=rng)

    burst_thread: threading.Thread | None = None
    if config.burst_enabled:
        burst_thread = threading.Thread(
            target=_burst_scheduler,
            args=(config, _shutdown),
            daemon=True,
        )
        burst_thread.start()

    batch: list[tuple[str, str, str, dict]] = []
    batch_size = 100
    event_count = 0
    last_log = time.monotonic()

    try:
        for event in source.events():
            if _shutdown.is_set():
                break

            batch.append((event.topic, event.key, event.schema_name, event.record))

            if len(batch) >= batch_size:
                producer.produce_batch(batch)
                event_count += len(batch)
                batch.clear()

                now = time.monotonic()
                if now - last_log >= 10.0:
                    logger.info("throughput", events=event_count, elapsed_s=round(now - last_log, 1))
                    event_count = 0
                    last_log = now
    except KeyboardInterrupt:
        pass
    finally:
        if batch:
            producer.produce_batch(batch)
            event_count += len(batch)
        producer.close()
        logger.info("simulator_stopped", final_events=event_count)


def _burst_scheduler(config: SimulatorConfig, shutdown: threading.Event) -> None:
    while not shutdown.is_set():
        shutdown.wait(timeout=config.burst_interval)
        if shutdown.is_set():
            break
        logger.info("burst_activated", duration=config.burst_duration)
        config.activate_burst()
        shutdown.wait(timeout=config.burst_duration)
        config.deactivate_burst()
        logger.info("burst_deactivated")


if __name__ == "__main__":
    main()
