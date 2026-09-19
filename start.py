#!/usr/bin/env python3

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CLIENT_POM = ROOT / "client" / "pom.xml"


def find_maven() -> str:
    candidates = ["mvn.cmd", "mvn.bat", "mvn"] if os.name == "nt" else ["mvn"]

    for candidate in candidates:
        path = shutil.which(candidate)
        if path:
            return path

    raise SystemExit("错误：未找到 Maven，请确认 mvn 已加入 PATH。")


def run(command: list[str], env: dict[str, str] | None = None) -> None:
    print()
    print(">", " ".join(command))

    result = subprocess.run(
        command,
        cwd=ROOT,
        env=env,
    )

    if result.returncode != 0:
        raise SystemExit(result.returncode)


def build(mvn: str) -> None:
    run(
        [
            mvn,
            "clean",
            "test",
        ]
    )


def install_client_dependencies(mvn: str) -> None:
    run(
        [
            mvn,
            "clean",
            "-pl",
            "client",
            "-am",
            "install",
            "-DskipTests",
        ]
    )


def run_client(mvn: str) -> None:
    install_client_dependencies(mvn)

    run(
        [
            mvn,
            "-f",
            str(CLIENT_POM),
            "org.openjfx:javafx-maven-plugin:0.0.8:run",
        ]
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Algorithms development launcher")

    target = parser.add_mutually_exclusive_group()
    target.add_argument("--build", action="store_true", help="执行完整 clean test")

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    mvn = find_maven()

    if args.build:
        build(mvn)
        return

    run_client(mvn)


if __name__ == "__main__":
    main()
