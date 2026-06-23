"""deephaven.ui chat interface for the local LLM agent.

Left panel: chat (user prompts, assistant markdown, collapsible tool calls).
Right panel: tabbed view of tables / figures the agent created.

The agent state is created in :func:`agent_chat` (outside any ``@ui.component``)
so it is shared across viewers, and the panels subscribe to it and re-render
when the background agent thread updates it.
"""

from __future__ import annotations

import os
from typing import Any, Sequence

from deephaven import ui  # type: ignore[attr-defined]

from ._client import OllamaClient
from .agent import Agent, AgentState, ChatMessage, OutputTab
from .config import AgentConfig, DEFAULT_CONFIG
from .docs import make_doc_search
from .executor import CodeExecutor
from .rag import DocIndex
from .tools import ToolBox
from . import skills


def _use_rerender(state: AgentState) -> None:
    """Re-render the calling component whenever ``state`` changes.

    Updates are marshalled onto the render thread via the render queue because
    the agent mutates state from a background thread.
    """
    _, set_tick = ui.use_state(0)
    render_queue = ui.use_render_queue()

    def subscribe():
        def listener():
            render_queue(lambda: set_tick(lambda v: v + 1))

        return state.subscribe(listener)

    ui.use_effect(subscribe, [state])


def _render_message(message: ChatMessage) -> Any:
    if message.role == "user":
        return ui.view(
            ui.text(message.content),
            background_color="accent-200",
            border_radius="medium",
            padding="size-150",
            align_self="end",
            max_width="90%",
        )
    if message.role == "assistant":
        return ui.view(
            ui.markdown(message.content),
            background_color="gray-100",
            border_radius="medium",
            padding="size-150",
            max_width="100%",
        )
    if message.role == "error":
        return ui.view(
            ui.text(message.content),
            background_color="negative-200",
            border_radius="medium",
            padding="size-150",
        )
    if message.role == "status":
        return ui.view(
            ui.markdown(f"_{message.content}_"),
            align_self="center",
            padding="size-100",
        )
    # Tool call invocation or result, shown collapsed.
    if message.tool_args is not None:
        title = f"Calling {message.tool_name}"
        body = ui.markdown(f"```json\n{message.tool_args}\n```")
    else:
        title = f"Result from {message.tool_name}"
        body = ui.markdown(f"```text\n{message.content}\n```")
    return ui.disclosure(title=title, panel=body)


@ui.component
def _chat_panel(state: AgentState, agent: Agent):
    _use_rerender(state)
    draft, set_draft = ui.use_state("")

    busy = state.busy

    def send():
        if draft.strip() and not busy:
            agent.submit(draft)
            set_draft("")

    messages = state.messages
    if messages:
        history: Any = ui.view(
            ui.flex(
                *[_render_message(m) for m in messages],
                direction="column",
                gap="size-150",
            ),
            overflow="auto",
            flex_grow=1,
            padding="size-100",
        )
    else:
        history = ui.view(
            ui.text(
                "Ask me to build tables, plots, or dashboards. "
                'For example: "Create a dashboard of World Cup statistics".'
            ),
            flex_grow=1,
            padding="size-200",
        )

    available_models = state.available_models
    if available_models:
        model_control: Any = ui.picker(
            *available_models,
            selected_key=state.model,
            on_change=lambda key: agent.set_model(str(key)),
            aria_label="Model",
            label="Model",
            label_position="side",
            is_disabled=busy,
            width="size-3600",
        )
    else:
        model_control = ui.text(f"Model: {state.model or 'unavailable'}")

    header = ui.flex(
        model_control,
        direction="row",
        justify_content="end",
        align_items="center",
        gap="size-100",
    )

    composer = ui.flex(
        ui.text_area(
            value=draft,
            on_change=set_draft,
            is_disabled=busy,
            label=None,
            flex_grow=1,
            aria_label="Message",
        ),
        (
            ui.button(
                "Stop",
                on_press=lambda: agent.cancel(),
                variant="negative",
            )
            if busy
            else ui.button(
                "Send",
                on_press=lambda: send(),
                variant="accent",
            )
        ),
        direction="row",
        gap="size-100",
        align_items="end",
    )

    return ui.panel(
        ui.flex(
            header,
            history,
            composer,
            direction="column",
            height="100%",
            gap="size-100",
        ),
        title="Agent Chat",
    )


def _unwrap_ui_value(value: Any) -> Any:
    """Prepare a deephaven.ui element for embedding in a tab.

    A ``ui.dashboard`` is a top-level layout and cannot be nested inside a tab,
    so render its inner content instead.
    """
    try:
        from deephaven.ui.elements import (  # type: ignore[import-not-found]
            DashboardElement,
        )

        if isinstance(value, DashboardElement):
            return value.render().get("children", value)
    except Exception:
        pass
    return value


def _output_tab(output: OutputTab) -> Any:
    if output.kind == "table":
        content = ui.table(output.value)
    elif output.kind == "ui":
        content = _unwrap_ui_value(output.value)
    else:
        content = output.value
    return ui.tab(content, title=output.title, key=output.key)


@ui.component
def _output_panel(state: AgentState):
    _use_rerender(state)
    outputs = state.outputs

    if not outputs:
        return ui.panel(
            ui.view(
                ui.text(
                    "Tables, figures, and ui components the agent creates will "
                    "appear here."
                ),
                padding="size-200",
            ),
            title="Output",
        )

    return ui.panel(
        ui.tabs(*[_output_tab(o) for o in outputs]),
        title="Output",
    )


@ui.component
def _agent_dashboard(state: AgentState, agent: Agent):
    return ui.row(
        _chat_panel(state, agent),
        _output_panel(state),
    )


def _make_doc_search(
    client: OllamaClient,
    docs_paths: Sequence[str],
    config: AgentConfig,
):
    index = DocIndex(docs_paths, client) if docs_paths else None
    return make_doc_search(
        index,
        web_fallback=config.web_fallback,
        min_score=config.doc_min_score,
        top_k=config.rag_top_k,
    )


def agent_chat(
    config: AgentConfig = DEFAULT_CONFIG,
    docs_paths: Sequence[str] | None = None,
    namespace: dict[str, Any] | None = None,
):
    """Create the agent chat widget.

    Args:
        config: Agent configuration (Ollama host, model, etc.).
        docs_paths: Directories of markdown docs to index for RAG. Defaults to
            the ``DH_AGENT_DOCS_PATHS`` environment variable (os.pathsep
            separated) if set, otherwise the bundled Deephaven query-writing
            skill so ``search_docs`` works out of the box.
        namespace: Optional dict used as the execution namespace for generated
            code. Pass ``globals()`` to let the agent share your session scope.

    Returns:
        A deephaven.ui dashboard. Assign it to a variable to open it.
    """
    if docs_paths is None:
        env_paths = os.environ.get("DH_AGENT_DOCS_PATHS", "")
        docs_paths = [p for p in env_paths.split(os.pathsep) if p]
    if not docs_paths:
        docs_paths = skills.skill_doc_paths()

    client = OllamaClient(config)
    state = AgentState()
    executor = CodeExecutor(namespace)
    doc_search = _make_doc_search(client, docs_paths, config)
    toolbox = ToolBox(
        executor=executor,
        on_outputs=state.add_outputs,
        doc_search=doc_search,
    )
    agent = Agent(state=state, client=client, toolbox=toolbox, config=config)

    # Populate the model picker with locally available models, ensuring the
    # active model is always selectable even if it is not installed yet.
    available = client.list_models()
    if client.model not in available:
        available = [client.model, *available]
    state.set_available_models(available)
    state.set_model(client.model)

    return ui.dashboard(_agent_dashboard(state, agent))
