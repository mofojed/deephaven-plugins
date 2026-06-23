"""Executes LLM-generated Deephaven code and captures renderable outputs.

Code runs in a persistent namespace shared across a chat session, so the model
can build on objects it created in earlier turns. After each execution we diff
the namespace to discover newly created (or rebound) tables / figures and return
them so the UI can display each in its own tab.
"""

from __future__ import annotations

import contextlib
import io
import traceback
from dataclasses import dataclass, field
from typing import Any


@dataclass
class CapturedOutput:
    """A renderable object produced by executed code."""

    name: str
    value: Any
    kind: str  # "table" | "figure" | "other"


@dataclass
class ExecutionResult:
    """Outcome of executing a snippet of code."""

    ok: bool
    stdout: str = ""
    error: str = ""
    outputs: list[CapturedOutput] = field(default_factory=list)

    def to_model_text(self) -> str:
        """Format the result as text to feed back to the model."""
        parts: list[str] = []
        if self.stdout.strip():
            parts.append(f"STDOUT:\n{self.stdout.strip()}")
        if self.outputs:
            names = ", ".join(f"{o.name} ({o.kind})" for o in self.outputs)
            parts.append(f"Created objects displayed to the user: {names}")
        if self.ok and not parts:
            parts.append("Code executed successfully with no output.")
        if not self.ok:
            parts.append(f"ERROR:\n{self.error.strip()}")
        return "\n\n".join(parts)


def _classify(value: Any) -> str | None:
    """Return the renderable kind of a value, or None if not renderable."""
    try:
        from deephaven.table import Table, PartitionedTable

        if isinstance(value, (Table, PartitionedTable)):
            return "table"
    except Exception:
        pass

    module = type(value).__module__ or ""
    type_name = type(value).__name__
    if module.startswith("deephaven.plot") or "Figure" in type_name:
        return "figure"
    return None


class CodeExecutor:
    """Runs code in a persistent namespace and captures renderable outputs."""

    def __init__(self, namespace: dict[str, Any] | None = None):
        self._namespace: dict[str, Any] = namespace if namespace is not None else {}
        # Seed with commonly used imports for convenience.
        self._bootstrap()

    def _bootstrap(self) -> None:
        exec_globals = self._namespace
        try:
            exec(
                "import deephaven\n"
                "from deephaven import empty_table, new_table, time_table, merge\n",
                exec_globals,
            )
        except Exception:
            # Deephaven may not be importable outside a server session (e.g. tests).
            pass

    @property
    def namespace(self) -> dict[str, Any]:
        return self._namespace

    def execute(self, code: str) -> ExecutionResult:
        """Execute ``code`` and return captured stdout, errors and outputs."""
        before = dict(self._namespace)
        stdout = io.StringIO()
        try:
            with contextlib.redirect_stdout(stdout):
                exec(code, self._namespace)
        except Exception:
            return ExecutionResult(
                ok=False,
                stdout=stdout.getvalue(),
                error=traceback.format_exc(),
            )

        outputs = self._capture_new_outputs(before)
        return ExecutionResult(ok=True, stdout=stdout.getvalue(), outputs=outputs)

    def _capture_new_outputs(self, before: dict[str, Any]) -> list[CapturedOutput]:
        outputs: list[CapturedOutput] = []
        for name, value in self._namespace.items():
            if name.startswith("_"):
                continue
            # Only capture newly created or rebound values.
            if name in before and before[name] is value:
                continue
            kind = _classify(value)
            if kind is not None:
                outputs.append(CapturedOutput(name=name, value=value, kind=kind))
        return outputs
