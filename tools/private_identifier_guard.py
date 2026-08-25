#!/usr/bin/env python3
"""Fail when a private identifier appears in a tracked text file."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ENV_NAME = "ETICHETTE_PRIVATE_DENYLIST"
MIN_IDENTIFIER_LENGTH = 4


def load_identifiers() -> tuple[str, ...]:
    raw = os.environ.get(ENV_NAME, "")
    identifiers = tuple(dict.fromkeys(line.strip() for line in raw.splitlines() if line.strip()))
    if not identifiers:
        raise RuntimeError(f"{ENV_NAME} is empty")
    too_short = [item for item in identifiers if len(item) < MIN_IDENTIFIER_LENGTH]
    if too_short:
        raise RuntimeError(
            f"{ENV_NAME} contains an identifier shorter than {MIN_IDENTIFIER_LENGTH} characters"
        )
    return identifiers


def tracked_files() -> list[Path]:
    result = subprocess.run(["git", "ls-files", "-z"], check=True, stdout=subprocess.PIPE)
    return [Path(item) for item in result.stdout.decode("utf-8").split("\0") if item]


def decode_text(data: bytes) -> str | None:
    if data.startswith((b"\xff\xfe", b"\xfe\xff")):
        try:
            return data.decode("utf-16")
        except UnicodeDecodeError:
            return None
    if b"\0" in data[:8192]:
        return None
    for encoding in ("utf-8-sig", "utf-8", "cp1252"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return None


def main() -> int:
    try:
        identifiers = load_identifiers()
        paths = tracked_files()
    except (RuntimeError, subprocess.CalledProcessError) as exc:
        print(f"private identifier guard: {exc}", file=sys.stderr)
        return 2

    violations: set[tuple[str, int]] = set()
    for path in paths:
        try:
            data = path.read_bytes()
        except OSError as exc:
            print(f"private identifier guard: cannot read {path}: {exc}", file=sys.stderr)
            return 2
        text = decode_text(data)
        if text is None:
            continue
        for identifier in identifiers:
            start = 0
            while True:
                index = text.find(identifier, start)
                if index < 0:
                    break
                line = text.count("\n", 0, index) + 1
                violations.add((path.as_posix(), line))
                start = index + len(identifier)

    if violations:
        for path, line in sorted(violations):
            print(f"{path}:{line}: private identifier detected", file=sys.stderr)
        print(f"private identifier guard: {len(violations)} location(s) rejected", file=sys.stderr)
        return 1

    print(f"private identifier guard: checked {len(paths)} tracked files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
