#!/usr/bin/env python3
"""Create GitHub issues from GSD PLAN.md files.

Parses PLAN.md task blocks and creates structured GitHub issues matching
the SPP (StablePay Pipeline) issue format with Summary, Business Context,
Technical Context, Implementation Guide, Acceptance Criteria, Dependencies,
and Phase & Batch sections.

Usage:
    uv run scripts/create-github-issues.py <phase-number> [--dry-run]

Examples:
    uv run scripts/create-github-issues.py 1 --dry-run   # preview issues
    uv run scripts/create-github-issues.py 2              # create issues
"""
# /// script
# requires-python = ">=3.13"
# dependencies = ["pyyaml>=6.0"]
# ///

import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

import yaml

REPO_ROOT = Path(__file__).resolve().parent.parent
PLANNING_DIR = REPO_ROOT / ".planning"
REQUIREMENTS_FILE = PLANNING_DIR / "REQUIREMENTS.md"
ROADMAP_FILE = PLANNING_DIR / "ROADMAP.md"
ISSUE_MAP_FILE = PLANNING_DIR / "issue-map.json"
PROJECT_PREFIX = "SPP"


@dataclass
class Task:
    task_id: str
    title: str
    task_type: str
    read_first: list[str] = field(default_factory=list)
    action: str = ""
    acceptance_criteria: list[str] = field(default_factory=list)
    plan_number: str = ""
    plan_title: str = ""
    phase_number: str = ""
    phase_name: str = ""
    requirements: list[str] = field(default_factory=list)
    depends_on: list[str] = field(default_factory=list)


def find_phase_dir(phase_num: str) -> Path | None:
    padded = phase_num.zfill(2)
    for d in PLANNING_DIR.glob("phases/*"):
        if d.is_dir() and d.name.startswith(f"{padded}-"):
            return d
    return None


def parse_frontmatter(content: str) -> dict:
    match = re.match(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not match:
        return {}
    return yaml.safe_load(match.group(1)) or {}


def parse_tasks(content: str, frontmatter: dict) -> list[Task]:
    tasks = []
    task_pattern = re.compile(
        r'<task\s+id="([^"]+)"\s+type="([^"]+)">\s*'
        r"<title>(.*?)</title>\s*"
        r"(?:<read_first>(.*?)</read_first>\s*)?"
        r"<action>(.*?)</action>\s*"
        r"(?:<acceptance_criteria>(.*?)</acceptance_criteria>\s*)?"
        r"</task>",
        re.DOTALL,
    )

    for m in task_pattern.finditer(content):
        task_id = m.group(1)
        task_type = m.group(2)
        title = m.group(3).strip()

        read_first_raw = m.group(4) or ""
        read_first = [
            line.strip().lstrip("- ") for line in read_first_raw.strip().splitlines() if line.strip().startswith("-")
        ]

        action = m.group(5).strip()

        ac_raw = m.group(6) or ""
        acceptance_criteria = [
            line.strip().lstrip("- ") for line in ac_raw.strip().splitlines() if line.strip().startswith("-")
        ]

        tasks.append(
            Task(
                task_id=task_id,
                title=title,
                task_type=task_type,
                read_first=read_first,
                action=action,
                acceptance_criteria=acceptance_criteria,
                plan_number=frontmatter.get("plan", ""),
                plan_title=frontmatter.get("title", ""),
                phase_number=str(frontmatter.get("phase", "")),
                requirements=frontmatter.get("requirements", []),
                depends_on=frontmatter.get("depends_on", []),
            )
        )

    return tasks


def load_requirements() -> dict[str, str]:
    if not REQUIREMENTS_FILE.exists():
        return {}
    content = REQUIREMENTS_FILE.read_text()
    reqs = {}
    for match in re.finditer(
        r"\*\*([A-Z]+-\d+)\*\*:\s*(.+?)(?=\n- \[|\n###|\n---|\Z)",
        content,
        re.DOTALL,
    ):
        req_id = match.group(1)
        req_text = match.group(2).strip()
        req_text = re.sub(r"\s+", " ", req_text)
        reqs[req_id] = req_text
    return reqs


def load_phase_name(phase_num: str) -> str:
    if not ROADMAP_FILE.exists():
        return ""
    content = ROADMAP_FILE.read_text()
    pattern = rf"\|\s*{phase_num}\s*\|\s*([^|]+)\s*\|"
    match = re.search(pattern, content)
    if match:
        return match.group(1).strip()
    return ""


def extract_summary(action: str) -> str:
    lines = action.strip().splitlines()
    summary_lines = []
    for line in lines:
        if line.strip().startswith("```"):
            break
        if line.strip():
            summary_lines.append(line.strip())
        if len(summary_lines) >= 3:
            break
    return " ".join(summary_lines) if summary_lines else lines[0].strip() if lines else ""


def extract_code_blocks(action: str) -> str:
    blocks = re.findall(r"(```[\s\S]*?```)", action)
    if not blocks:
        return ""
    return "\n\n".join(blocks)


def format_issue_body(task: Task, reqs: dict[str, str], phase_name: str, issue_map: dict) -> str:
    sections = []

    # Summary
    summary = extract_summary(task.action)
    sections.append(f"## Summary\n\n{summary}")

    # Business Context — from mapped requirements
    if task.requirements:
        biz_lines = []
        for req_id in task.requirements:
            if req_id in reqs:
                biz_lines.append(f"- **{req_id}**: {reqs[req_id][:200]}")
        if biz_lines:
            sections.append("## Business Context\n\n" + "\n".join(biz_lines))

    # Technical Context — from read_first references
    if task.read_first:
        tech_lines = ["Referenced specifications:"]
        for ref in task.read_first:
            tech_lines.append(f"- `{ref}`")
        sections.append("## Technical Context\n\n" + "\n".join(tech_lines))

    # Implementation Guide — full action with code blocks
    impl = task.action
    if len(impl) > 3000:
        impl = impl[:3000] + "\n\n_(truncated — see PLAN.md for full details)_"
    sections.append(f"## Implementation Guide\n\n{impl}")

    # Acceptance Criteria
    if task.acceptance_criteria:
        ac_lines = [f"- [ ] {criterion}" for criterion in task.acceptance_criteria]
        ac_lines.append(f"- [ ] `./gradlew build` passes (if Java touched)")
        sections.append("## Acceptance Criteria\n\n" + "\n".join(ac_lines))

    # Dependencies
    dep_lines = []
    if task.depends_on:
        for dep_plan in task.depends_on:
            dep_key = f"plan-{task.phase_number}-{dep_plan}"
            if dep_key in issue_map:
                dep_lines.append(f"- Plan {dep_plan} issues (see milestone)")
    task_parts = task.task_id.split(".")
    if len(task_parts) == 3:
        task_seq = int(task_parts[2])
        if task_seq > 1:
            prev_id = f"{task_parts[0]}.{task_parts[1]}.{task_seq - 1}"
            if prev_id in issue_map:
                dep_lines.append(f"- #{issue_map[prev_id]} ({prev_id})")
    if dep_lines:
        sections.append("## Dependencies\n\n" + "\n".join(dep_lines))

    # Phase & Batch
    sections.append(
        f"## Phase & Batch\n\nPhase {task.phase_number} — {phase_name}, Plan {task.plan_number} ({task.plan_title})"
    )

    return "\n\n".join(sections)


def get_or_create_label(label: str, color: str, description: str = "") -> None:
    result = subprocess.run(
        ["gh", "label", "create", label, "--color", color, "--description", description, "--force"],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0 and "already exists" not in result.stderr:
        print(f"  Warning: failed to create label '{label}': {result.stderr.strip()}")


def get_or_create_milestone(title: str, description: str = "") -> None:
    result = subprocess.run(
        ["gh", "api", "repos/{owner}/{repo}/milestones", "--method", "GET"],
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        milestones = json.loads(result.stdout)
        for ms in milestones:
            if ms["title"] == title:
                return

    subprocess.run(
        [
            "gh",
            "api",
            "repos/{owner}/{repo}/milestones",
            "--method",
            "POST",
            "-f",
            f"title={title}",
            "-f",
            f"description={description}",
        ],
        capture_output=True,
        text=True,
    )


def create_issue(title: str, body: str, labels: list[str], milestone: str) -> int | None:
    cmd = [
        "gh",
        "issue",
        "create",
        "--title",
        title,
        "--body",
        body,
        "--milestone",
        milestone,
    ]
    for label in labels:
        cmd.extend(["--label", label])

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  Error creating issue: {result.stderr.strip()}")
        return None

    url = result.stdout.strip()
    match = re.search(r"/(\d+)$", url)
    if match:
        return int(match.group(1))
    return None


def load_issue_map() -> dict:
    if ISSUE_MAP_FILE.exists():
        return json.loads(ISSUE_MAP_FILE.read_text())
    return {}


def save_issue_map(issue_map: dict) -> None:
    ISSUE_MAP_FILE.write_text(json.dumps(issue_map, indent=2) + "\n")


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: uv run scripts/create-github-issues.py <phase-number> [--dry-run]")
        sys.exit(1)

    phase_num = sys.argv[1]
    dry_run = "--dry-run" in sys.argv

    phase_dir = find_phase_dir(phase_num)
    if not phase_dir:
        print(f"Error: Phase {phase_num} directory not found under .planning/phases/")
        sys.exit(1)

    phase_name = load_phase_name(phase_num)
    reqs = load_requirements()
    issue_map = load_issue_map()

    plan_files = sorted(phase_dir.glob("*-PLAN.md"))
    if not plan_files:
        print(f"No PLAN.md files found in {phase_dir}")
        sys.exit(1)

    all_tasks: list[Task] = []
    for plan_file in plan_files:
        content = plan_file.read_text()
        frontmatter = parse_frontmatter(content)
        frontmatter["phase_name"] = phase_name
        tasks = parse_tasks(content, frontmatter)
        all_tasks.extend(tasks)

    if not all_tasks:
        print("No tasks found in PLAN.md files")
        sys.exit(1)

    print(f"\n{'DRY RUN — ' if dry_run else ''}Creating issues for Phase {phase_num}: {phase_name}")
    print(f"Found {len(all_tasks)} tasks across {len(plan_files)} plan(s)\n")

    # Compute next SPP number
    existing_numbers = [
        int(re.search(r"SPP-(\d+)", v).group(1))
        for v in issue_map.values()
        if isinstance(v, str) and re.search(r"SPP-(\d+)", v)
    ]
    next_number = max(existing_numbers, default=0) + 1

    if not dry_run:
        padded = phase_num.zfill(2)
        phase_label = f"phase:{padded}-{phase_name.lower().replace(' ', '-').replace('&', 'and')}"
        milestone_title = f"Phase {phase_num}: {phase_name}"

        print("Setting up labels and milestone...")
        get_or_create_label(phase_label, "0075ca", f"Phase {phase_num}")
        get_or_create_label("priority:high", "d73a4a")
        get_or_create_label("priority:medium", "fbca04")
        get_or_create_label("priority:low", "0e8a16")
        for plan_file in plan_files:
            fm = parse_frontmatter(plan_file.read_text())
            plan_num = fm.get("plan", "")
            get_or_create_label(f"plan:{padded}-{plan_num}", "c5def5", fm.get("title", ""))
        get_or_create_milestone(milestone_title, f"Phase {phase_num} of stablepay-payment-pipeline")
        print()

    for task in all_tasks:
        spp_id = f"{PROJECT_PREFIX}-{next_number:02d}"
        issue_title = f"{spp_id}: {task.title}"
        padded = phase_num.zfill(2)
        phase_slug = phase_name.lower().replace(" ", "-").replace("&", "and")
        labels = [
            f"phase:{padded}-{phase_slug}",
            f"plan:{padded}-{task.plan_number}",
            "priority:high" if task.task_type == "execute" else "priority:medium",
        ]
        milestone_title = f"Phase {phase_num}: {phase_name}"

        body = format_issue_body(task, reqs, phase_name, issue_map)

        if dry_run:
            print(f"{'─' * 60}")
            print(f"  {issue_title}")
            print(f"  Labels: {', '.join(labels)}")
            print(f"  Milestone: {milestone_title}")
            print(f"  Acceptance Criteria: {len(task.acceptance_criteria)} items")
            print(f"  Requirements: {', '.join(task.requirements) if task.requirements else 'none'}")
            print()
        else:
            print(f"Creating {issue_title}...", end=" ", flush=True)
            issue_num = create_issue(issue_title, body, labels, milestone_title)
            if issue_num:
                issue_map[task.task_id] = f"{spp_id}:#{issue_num}"
                print(f"#{issue_num}")
            else:
                print("FAILED")

        next_number += 1

    if not dry_run:
        save_issue_map(issue_map)
        print(f"\nIssue map saved to {ISSUE_MAP_FILE}")

    print(f"\n{'Would create' if dry_run else 'Created'} {len(all_tasks)} issues")
    if dry_run:
        print("\nRun without --dry-run to create issues on GitHub.")


if __name__ == "__main__":
    main()
