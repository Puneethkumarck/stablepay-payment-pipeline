"""CLI for DLQ inspection and replay."""

from __future__ import annotations

from typing import Optional

import typer
from rich.console import Console
from rich.table import Table

from stablepay_dlq_tools.reader import get_dlq_event, list_dlq_events
from stablepay_dlq_tools.replayer import TRANSFORMS, replay_batch, replay_event

app = typer.Typer(name="dlq", help="Dead letter queue inspection and replay tools.")
console = Console()


@app.command()
def list_(
    error_class: Optional[str] = typer.Option(None, "--error-class", "-e", help="Filter by error class"),
    source_topic: Optional[str] = typer.Option(None, "--topic", "-t", help="Filter by source topic"),
    limit: int = typer.Option(50, "--limit", "-n", help="Maximum rows to return"),
) -> None:
    """List DLQ events from the Iceberg dlq_events table."""
    events = list_dlq_events(error_class=error_class, source_topic=source_topic, limit=limit)

    if not events:
        console.print("[yellow]No DLQ events found.[/yellow]")
        raise typer.Exit()

    table = Table(title=f"DLQ Events ({len(events)} rows)")
    table.add_column("event_id", style="cyan", max_width=36)
    table.add_column("source_topic", style="green")
    table.add_column("error_class", style="red")
    table.add_column("failed_at")
    table.add_column("retry_count", justify="right")

    for ev in events:
        table.add_row(
            str(ev.get("event_id", "")),
            str(ev.get("source_topic", "")),
            str(ev.get("error_class", "")),
            str(ev.get("failed_at", "")),
            str(ev.get("retry_count", "")),
        )

    console.print(table)


@app.command()
def inspect(
    event_id: str = typer.Argument(help="Event ID to inspect"),
) -> None:
    """Inspect a single DLQ event by ID."""
    event = get_dlq_event(event_id)

    if event is None:
        console.print(f"[red]Event {event_id} not found.[/red]")
        raise typer.Exit(code=1)

    for key, value in event.items():
        console.print(f"[bold]{key}:[/bold] {value}")


@app.command()
def replay(
    event_id: str = typer.Argument(help="Event ID to replay"),
    transform: str = typer.Option("identity", "--transform", "-x", help="Transform to apply"),
    target_topic: Optional[str] = typer.Option(None, "--target-topic", help="Override target topic"),
) -> None:
    """Replay a single DLQ event back to its source topic."""
    if transform not in TRANSFORMS:
        console.print(f"[red]Unknown transform: {transform}. Available: {', '.join(TRANSFORMS)}[/red]")
        raise typer.Exit(code=1)

    event = get_dlq_event(event_id)
    if event is None:
        console.print(f"[red]Event {event_id} not found.[/red]")
        raise typer.Exit(code=1)

    ok = replay_event(event, transform=TRANSFORMS[transform], target_topic=target_topic)
    if ok:
        console.print(f"[green]Replayed event {event_id} successfully.[/green]")
    else:
        console.print(f"[red]Failed to replay event {event_id}.[/red]")
        raise typer.Exit(code=1)


@app.command()
def replay_class(
    error_class: str = typer.Argument(help="Error class to replay all events for"),
    transform: str = typer.Option("identity", "--transform", "-x", help="Transform to apply"),
    target_topic: Optional[str] = typer.Option(None, "--target-topic", help="Override target topic"),
    limit: int = typer.Option(100, "--limit", "-n", help="Maximum events to replay"),
    dry_run: bool = typer.Option(False, "--dry-run", help="Show events without replaying"),
) -> None:
    """Replay all DLQ events matching an error class."""
    if transform not in TRANSFORMS:
        console.print(f"[red]Unknown transform: {transform}. Available: {', '.join(TRANSFORMS)}[/red]")
        raise typer.Exit(code=1)

    events = list_dlq_events(error_class=error_class, limit=limit)
    if not events:
        console.print(f"[yellow]No DLQ events found for error class: {error_class}[/yellow]")
        raise typer.Exit()

    console.print(f"Found [bold]{len(events)}[/bold] events for error class [red]{error_class}[/red]")

    if dry_run:
        for ev in events:
            console.print(f"  [cyan]{ev.get('event_id')}[/cyan] topic={ev.get('source_topic')}")
        console.print("[yellow]Dry run — no events replayed.[/yellow]")
        raise typer.Exit()

    success, failed = replay_batch(events, transform=TRANSFORMS[transform], target_topic=target_topic)
    console.print(f"[green]Replayed: {success}[/green]  [red]Failed: {failed}[/red]")
