#!/usr/bin/env python3
"""Regenerate Python dataclasses from Avro schemas.

Walks schemas/src/main/avro/**/*.avsc and generates dataclass files into
packages/schemas-py/src/_generated/.
"""

import json
import sys
from pathlib import Path

SCHEMAS_DIR = Path("schemas/src/main/avro")
OUTPUT_DIR = Path("packages/schemas-py/src/_generated")


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    schema_files = sorted(SCHEMAS_DIR.rglob("*.avsc"))
    if not schema_files:
        print("No .avsc files found under schemas/src/main/avro/", file=sys.stderr)
        sys.exit(1)

    generated = []
    for avsc_path in schema_files:
        with open(avsc_path) as f:
            schema = json.load(f)

        name = schema.get("name", avsc_path.stem)
        namespace = schema.get("namespace", "")
        module_name = name.lower()

        fields = schema.get("fields", [])
        lines = [
            f'"""Auto-generated from {avsc_path.relative_to(SCHEMAS_DIR)}. Do not edit."""',
            "from __future__ import annotations",
            "from dataclasses import dataclass",
            "from typing import Optional",
            "",
            "",
            "@dataclass",
            f"class {name}:",
            f'    """{namespace}.{name}"""',
        ]

        if not fields:
            lines.append("    pass")
        else:
            for field in fields:
                field_name = field["name"]
                field_type = _avro_type_to_python(field["type"])
                default = ""
                if isinstance(field.get("type"), list) and field["type"][0] == "null":
                    default = " = None"
                lines.append(f"    {field_name}: {field_type}{default}")

        out_path = OUTPUT_DIR / f"{module_name}.py"
        out_path.write_text("\n".join(lines) + "\n")
        generated.append(name)

    init_content = '"""Auto-generated Avro schema Python bindings. Do not edit."""\n\n'
    init_content += "\n".join(
        f"from stablepay_schemas._generated.{name.lower()} import {name}" for name in sorted(generated)
    )
    init_content += "\n"
    (OUTPUT_DIR / "__init__.py").write_text(init_content)

    print(f"Generated {len(generated)} Python schema classes in {OUTPUT_DIR}")


def _avro_type_to_python(avro_type: object) -> str:
    if isinstance(avro_type, str):
        mapping = {
            "string": "str",
            "long": "int",
            "int": "int",
            "boolean": "bool",
            "double": "float",
            "bytes": "bytes",
            "float": "float",
        }
        return mapping.get(avro_type, "str")
    if isinstance(avro_type, list):
        non_null = [t for t in avro_type if t != "null"]
        if len(non_null) == 1:
            return f"Optional[{_avro_type_to_python(non_null[0])}]"
        return "object"
    if isinstance(avro_type, dict):
        logical = avro_type.get("logicalType", "")
        if "timestamp" in logical:
            return "int"
        if avro_type.get("type") == "enum":
            return "str"
        if avro_type.get("type") == "array":
            return "list"
        if avro_type.get("type") == "record":
            return avro_type.get("name", "object")
        return _avro_type_to_python(avro_type.get("type", "string"))
    return "object"


if __name__ == "__main__":
    main()
