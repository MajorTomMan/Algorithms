#!/usr/bin/env python3

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

import psutil

ROOT = Path(__file__).resolve().parent
SCRIPT = Path(__file__).resolve()
CLIENT_POM = ROOT / "client" / "pom.xml"


# ============================================================
# Platform
# ============================================================


def is_windows() -> bool:
    return os.name == "nt"


def is_linux() -> bool:
    return sys.platform.startswith("linux")


def platform_name() -> str:
    if is_windows():
        return "Windows"
    if is_linux():
        return "Linux"
    raise SystemExit(f"不支持的操作系统：{sys.platform}")


# ============================================================
# Maven
# ============================================================


def find_maven() -> str:
    candidates = ["mvn.cmd", "mvn.bat", "mvn"] if is_windows() else ["mvn"]

    for candidate in candidates:
        path = shutil.which(candidate)
        if path:
            return path

    raise SystemExit("错误：未找到 Maven，请确认 mvn 已加入 PATH。")


def run(command: list[str]) -> None:
    print()
    print(">", " ".join(command), flush=True)

    result = subprocess.run(
        command,
        cwd=ROOT,
        check=False,
    )

    if result.returncode != 0:
        raise SystemExit(result.returncode)


# ============================================================
# Process detection
# ============================================================


def same_path(a: Path, b: Path) -> bool:
    a = str(a.resolve())
    b = str(b.resolve())

    if is_windows():
        return os.path.normcase(a) == os.path.normcase(b)

    return a == b


def is_previous_launcher(proc: psutil.Process) -> bool:
    if proc.pid == os.getpid():
        return False

    try:
        name = proc.name().lower()

        if not (name.startswith("python") or name in {"py.exe", "py"}):
            return False

        args = proc.cmdline()
        cwd = Path(proc.cwd())

        for arg in args[1:]:
            if not arg.lower().endswith(".py"):
                continue

            candidate = Path(arg)

            if not candidate.is_absolute():
                candidate = cwd / candidate

            if same_path(candidate, SCRIPT):
                return True

    except (
        psutil.NoSuchProcess,
        psutil.AccessDenied,
        OSError,
    ):
        pass

    return False


# ============================================================
# Windows: taskkill /T /F
# ============================================================


def kill_windows(proc: psutil.Process) -> None:
    print(f"[Windows] 结束旧实例 PID={proc.pid}")

    result = subprocess.run(
        [
            "taskkill",
            "/PID",
            str(proc.pid),
            "/T",
            "/F",
        ],
        capture_output=True,
        text=True,
        errors="replace",
        check=False,
    )

    if result.returncode != 0:
        if psutil.pid_exists(proc.pid):
            raise SystemExit(
                f"结束旧实例失败 PID={proc.pid}\n" f"{result.stdout}\n{result.stderr}"
            )
        return

    try:
        proc.wait(timeout=8)
    except psutil.NoSuchProcess:
        pass
    except psutil.TimeoutExpired:
        raise SystemExit(f"旧实例 PID={proc.pid} 未能在规定时间内退出。")


# ============================================================
# Linux: recursive process termination
# ============================================================


def kill_linux(proc: psutil.Process) -> None:
    print(f"[Linux] 结束旧实例 PID={proc.pid}")

    try:
        children = proc.children(recursive=True)
        targets = children[::-1] + [proc]
    except psutil.NoSuchProcess:
        return

    for target in targets:
        try:
            target.terminate()
        except psutil.NoSuchProcess:
            pass
        except psutil.AccessDenied:
            print(f"无权终止 PID={target.pid}")

    _, alive = psutil.wait_procs(targets, timeout=3)

    for target in alive:
        try:
            print(f"强制结束 PID={target.pid}")
            target.kill()
        except psutil.NoSuchProcess:
            pass
        except psutil.AccessDenied:
            print(f"无权强制结束 PID={target.pid}")

    _, alive = psutil.wait_procs(alive, timeout=5)

    if alive:
        pids = ", ".join(str(p.pid) for p in alive)
        raise SystemExit(f"旧实例仍未完全退出，停止清理和启动：{pids}")


# ============================================================
# Stop previous application
# ============================================================


def kill_previous_instance() -> None:
    print()
    print("========== 检查旧实例 ==========")

    previous = [proc for proc in psutil.process_iter() if is_previous_launcher(proc)]

    if not previous:
        print("未发现旧启动实例。")
        return

    for proc in previous:
        if is_windows():
            kill_windows(proc)
        elif is_linux():
            kill_linux(proc)

    print("旧实例终止完成。")


# ============================================================
# Build and launch
# ============================================================


def build(mvn: str) -> None:
    print()
    print("========== Clean & Test ==========")

    run(
        [
            mvn,
            "clean",
            "test",
        ]
    )


def install_client_dependencies(mvn: str) -> None:
    print()
    print("========== Clean & Install ==========")

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

    print()
    print("========== 启动 JavaFX ==========")

    run(
        [
            mvn,
            "-f",
            str(CLIENT_POM),
            "org.openjfx:javafx-maven-plugin:0.0.8:run",
        ]
    )


# ============================================================
# Main
# ============================================================


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Algorithms development launcher")

    parser.add_argument(
        "--build",
        action="store_true",
        help="终止旧实例后执行完整 clean test",
    )

    return parser.parse_args()


def main() -> None:
    args = parse_args()

    print(f"当前平台：{platform_name()}")
    print(f"项目路径：{ROOT}")

    mvn = find_maven()

    # 先终止旧实例，避免 Windows 文件占用或旧进程干扰新构建。
    kill_previous_instance()

    if args.build:
        build(mvn)
        return

    run_client(mvn)


if __name__ == "__main__":
    main()
