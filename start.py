#!/usr/bin/env python3

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

import psutil

ROOT = Path(__file__).resolve().parent
SCRIPT = Path(__file__).resolve()
CLIENT_POM = ROOT / "client" / "pom.xml"
PID_FILE = ROOT / ".start-process.json"
POLL_SECONDS = 0.5


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
# Persisted process identity
# ============================================================


def process_identity(proc: psutil.Process) -> dict:
    return {"pid": proc.pid, "created_at": proc.create_time()}


def resolve_process(identity: dict | None) -> psutil.Process | None:
    if not isinstance(identity, dict):
        return None

    pid = identity.get("pid")
    created_at = identity.get("created_at")
    if not isinstance(pid, int) or isinstance(pid, bool) or pid <= 0:
        return None
    if not isinstance(created_at, (int, float)) or pid == os.getpid():
        return None

    try:
        proc = psutil.Process(pid)
        if abs(proc.create_time() - created_at) > 0.01:
            return None
        if not proc.is_running() or proc.status() == psutil.STATUS_ZOMBIE:
            return None
        return proc
    except (psutil.NoSuchProcess, psutil.AccessDenied, OSError):
        return None


def read_pid_file() -> dict | None:
    try:
        state = json.loads(PID_FILE.read_text(encoding="utf-8"))
        if isinstance(state, dict):
            return state
    except FileNotFoundError:
        return None
    except (OSError, UnicodeError, json.JSONDecodeError):
        pass
    print(f"PID 文件无效，将使用进程扫描兜底：{PID_FILE}")
    return None


def save_pid_file(state: dict) -> None:
    temporary = PID_FILE.with_name(f".start-process-{os.getpid()}.tmp")
    try:
        temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary.replace(PID_FILE)
    finally:
        temporary.unlink(missing_ok=True)


def delete_pid_file_if_owned(state: dict) -> None:
    current = read_pid_file()
    if current is not None and current.get("script") == state.get("script") \
            and current.get("launcher") == state.get("launcher"):
        PID_FILE.unlink(missing_ok=True)


def recorded_processes(state: dict | None) -> list[psutil.Process]:
    if state is None or not isinstance(state.get("script"), str):
        return []
    if not same_path(Path(state["script"]), SCRIPT):
        print("PID 文件属于其他项目路径，忽略其中的进程。")
        return []

    identities = [state.get("maven")]
    children = state.get("children", [])
    if isinstance(children, list):
        identities.extend(children)
    identities.append(state.get("launcher"))

    processes = []
    for identity in identities:
        proc = resolve_process(identity)
        if proc is not None:
            processes.append(proc)
    return processes


def terminate_instances(processes: list[psutil.Process]) -> None:
    seen = set()
    for proc in processes:
        if proc.pid == os.getpid() or proc.pid in seen:
            continue
        seen.add(proc.pid)
        try:
            if is_windows():
                kill_windows(proc)
            else:
                kill_linux(proc)
        except psutil.NoSuchProcess:
            pass


# ============================================================
# Stop previous application
# ============================================================


def kill_previous_instance() -> None:
    print()
    print("========== 检查旧实例 ==========")

    state = read_pid_file()
    previous = recorded_processes(state)
    previous.extend(proc for proc in psutil.process_iter() if is_previous_launcher(proc))

    if previous:
        terminate_instances(previous)
        print("旧实例终止完成。")
    else:
        print("未发现旧启动实例。")

    if state is not None and isinstance(state.get("script"), str) \
            and same_path(Path(state["script"]), SCRIPT):
        delete_pid_file_if_owned(state)


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
    command = [
        mvn,
        "-f",
        str(CLIENT_POM),
        "org.openjfx:javafx-maven-plugin:0.0.8:run",
    ]
    print(">", " ".join(command), flush=True)

    process = subprocess.Popen(command, cwd=ROOT)
    state = {
        "version": 1,
        "script": str(SCRIPT),
        "launcher": process_identity(psutil.Process(os.getpid())),
        "maven": None,
        "children": [],
    }

    try:
        state["maven"] = process_identity(psutil.Process(process.pid))
        save_pid_file(state)
        print(f"已记录启动进程：Maven PID={process.pid}，PID 文件={PID_FILE}")

        while process.poll() is None:
            try:
                children = psutil.Process(process.pid).children(recursive=True)
                identities = [process_identity(child) for child in children]
            except (psutil.NoSuchProcess, psutil.AccessDenied, OSError):
                identities = state["children"]

            if identities != state["children"]:
                state["children"] = identities
                save_pid_file(state)
            time.sleep(POLL_SECONDS)

        if process.returncode != 0:
            raise SystemExit(process.returncode)
    except KeyboardInterrupt:
        print("\n正在关闭 JavaFX 及其启动进程...")
        raise SystemExit(130)
    finally:
        # Maven 正常退出、Ctrl+C 或启动失败时，不遗留已经记录的子进程。
        try:
            terminate_instances(recorded_processes(state))
        except BaseException:
            print(f"进程清理失败，保留 PID 文件以便下次重试：{PID_FILE}")
            raise
        delete_pid_file_if_owned(state)


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

    parser.add_argument(
        "--stop",
        action="store_true",
        help="只终止上次启动的 JavaFX/Maven 实例，不启动新程序",
    )

    return parser.parse_args()


def main() -> None:
    args = parse_args()

    print(f"当前平台：{platform_name()}")
    print(f"项目路径：{ROOT}")

    if args.build and args.stop:
        raise SystemExit("--build 和 --stop 不能同时使用")

    # 先终止旧实例，避免 Windows 文件占用或旧进程干扰新构建。
    kill_previous_instance()
    if args.stop:
        return

    mvn = find_maven()
    if args.build:
        build(mvn)
        return

    run_client(mvn)


if __name__ == "__main__":
    main()
