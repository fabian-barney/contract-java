"""Compile clean classpath/JPMS consumers and run old generated code on new runtime."""

import hashlib
import os
import subprocess
import tempfile
from pathlib import Path

from release_common import CENTRAL, fetch, require

SOURCE = """package consumer;
import media.barney.contract.Contract;
public class Main {
  @Contract.Positive static int metadataOnly = -1;
  static int positive(@Contract.Positive int value) { return value; }
  @Contract.Positive static int result(int value) { return value; }
  static String password(@Contract.Mask @Contract.Pattern(regexp="[0-9]+") String value) { return value; }
  public static void main(String[] args) {
    if (positive(1) != 1 || password(null) != null || metadataOnly != -1) throw new AssertionError();
    try { positive(0); throw new AssertionError("Missing precondition"); }
    catch (IllegalArgumentException expected) { }
    try { result(-1); throw new AssertionError("Missing postcondition"); }
    catch (IllegalStateException expected) { }
    try { password("secret"); throw new AssertionError("Missing masked precondition"); }
    catch (IllegalArgumentException expected) {
      if (expected.getMessage().contains("secret") || !expected.getMessage().contains("[MASKED]")) throw new AssertionError();
    }
  }
}
"""


def consumers(version: str) -> None:
    current = Path(f"contract-core/target/contract-core-{version}.jar").resolve()
    repository = Path.home() / ".m2/repository"
    annotations = [
        repository / "org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar",
        repository / "org/jspecify/jspecify/1.0.1/jspecify-1.0.1.jar",
    ]
    require(
        current.is_file() and all(path.is_file() for path in annotations),
        "Missing consumer dependencies",
    )
    exports = [
        f"-J--add-exports=jdk.compiler/com.sun.tools.javac.{package}=ALL-UNNAMED"
        for package in ("code", "api", "parser", "processing", "tree", "util")
    ]
    with tempfile.TemporaryDirectory(prefix="contract-consumer-") as temp:
        root = Path(temp)
        old = root / "contract-core-0.1.14.jar"
        url = f"{CENTRAL}/media/barney/contract-core/0.1.14/{old.name}"
        data = fetch(url)
        require(
            hashlib.sha512(data).hexdigest() == fetch(url + ".sha512").decode().strip(),
            "Old consumer artifact checksum mismatch",
        )
        old.write_bytes(data)
        source = root / "consumer/Main.java"
        source.parent.mkdir()
        source.write_text(SOURCE, encoding="utf-8")
        for label, processor in (("current", current), ("previous", old)):
            classes = root / label
            classpath = os.pathsep.join(map(str, [processor] + annotations))
            subprocess.run(
                [
                    "javac",
                    *exports,
                    "-proc:full",
                    "--release",
                    "17",
                    "-classpath",
                    classpath,
                    "-processorpath",
                    classpath,
                    "-d",
                    str(classes),
                    str(source),
                ],
                check=True,
            )
            runtime = os.pathsep.join(map(str, [classes, current] + annotations))
            subprocess.run(["java", "-classpath", runtime, "consumer.Main"], check=True)
        descriptor = root / "module-info.java"
        descriptor.write_text(
            "module consumer { requires media.barney.contract.core; }\n",
            encoding="utf-8",
        )
        modulepath = os.pathsep.join(map(str, [current] + annotations))
        classes = root / "jpms"
        subprocess.run(
            [
                "javac",
                *exports,
                "-proc:full",
                "--release",
                "17",
                "--module-path",
                modulepath,
                "-processorpath",
                modulepath,
                "-d",
                str(classes),
                str(source),
                str(descriptor),
            ],
            check=True,
        )
        subprocess.run(
            [
                "java",
                "--module-path",
                os.pathsep.join([str(classes), modulepath]),
                "--module",
                "consumer/consumer.Main",
            ],
            check=True,
        )
    print(
        "Clean processor discovery, JPMS consumer, and v0.1.14 generated-code compatibility passed."
    )
