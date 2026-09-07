#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent
CLIENT_POM = ROOT / "client" / "pom.xml"


def find_maven() -> str:
    if os.name == "nt":
        candidates = ["mvn.cmd", "mvn.bat", "mvn"]
    else:
        candidates = ["mvn"]

    for candidate in candidates:
        path = shutil.which(candidate)
        if path:
            return path

    print("错误：未找到 Maven，请确认 mvn 已加入 PATH。")
    sys.exit(1)


def run(command: list[str]) -> None:
    print()
    print(">", " ".join(command))
    result = subprocess.run(command, cwd=ROOT)
    if result.returncode != 0:
        sys.exit(result.returncode)


def main() -> None:
    if not CLIENT_POM.exists():
        print(f"错误：找不到 {CLIENT_POM}")
        sys.exit(1)

    mvn = find_maven()

    # 先构建并安装 core / algorithms / client，
    # 让后续单独使用 client/pom.xml 启动时可以解析项目内部依赖。
    run([
        mvn,
        "-pl", "client",
        "-am",
        "install",
        "-DskipTests",
    ])

    # 从根目录启动 JavaFX Client。
    # 使用完整插件坐标，避免 Maven 的 javafx 前缀解析问题。
    run([
        mvn,
        "-f", str(CLIENT_POM),
        "org.openjfx:javafx-maven-plugin:0.0.8:run",
    ])


if __name__ == "__main__":
    main()
