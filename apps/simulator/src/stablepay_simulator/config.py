from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import yaml


@dataclass
class SimulatorConfig:
    rate: int = 200
    delay_multiplier: float = 0.01
    burst_enabled: bool = False
    burst_interval: int = 600
    burst_duration: int = 30
    customer_pool_size: int = 100
    seed: int | None = None
    flow_weights: dict[str, float] = field(default_factory=dict)
    burst_active: bool = False

    def activate_burst(self) -> None:
        self.burst_active = True

    def deactivate_burst(self) -> None:
        self.burst_active = False


def load_config(config_dir: Path, cli_overrides: dict | None = None) -> SimulatorConfig:
    overrides = cli_overrides or {}
    defaults_path = config_dir / "defaults.yaml"
    scenarios_path = config_dir / "scenarios.yaml"

    defaults: dict = {}
    if defaults_path.exists():
        defaults = yaml.safe_load(defaults_path.read_text()) or {}

    flow_weights: dict[str, float] = {}
    if scenarios_path.exists():
        scenarios_data = yaml.safe_load(scenarios_path.read_text()) or {}
        flow_weights = scenarios_data.get("flow_weights", {})

    merged = {**defaults, **{k: v for k, v in overrides.items() if v is not None}}

    return SimulatorConfig(
        rate=int(merged.get("rate", 200)),
        delay_multiplier=float(merged.get("delay_multiplier", 0.01)),
        burst_enabled=bool(merged.get("burst_enabled", False)),
        burst_interval=int(merged.get("burst_interval", 600)),
        burst_duration=int(merged.get("burst_duration", 30)),
        customer_pool_size=int(merged.get("customer_pool_size", 100)),
        seed=merged.get("seed"),
        flow_weights=flow_weights,
    )
