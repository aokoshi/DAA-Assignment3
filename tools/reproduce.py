"""One command for the complete build -> test -> benchmark -> report workflow."""
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def run(command):
    print("Running:", " ".join(str(part) for part in command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main():
    maven = shutil.which("mvn.cmd" if os.name == "nt" else "mvn")
    java = shutil.which("java")
    if os.environ.get("JAVA_HOME"):
        java = str(Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java"))
    if not maven or not java:
        raise SystemExit("Install JDK 17+ and Maven 3.9+, and add Maven to PATH; set JAVA_HOME to the JDK.")
    run([maven, "-B", "clean", "verify"])
    run([java, "-Xmx2g", "-jar", "target/daa-assignment3-1.0.jar", "benchmark"])
    run([sys.executable, "tools/analyze_results.py"])
    run([sys.executable, "tools/build_report.py"])


if __name__ == "__main__":
    main()
