# Deephaven LLM Agent (`deephaven.dh_agent`)

An experimental, pure [`deephaven.ui`](../ui) widget for hosting your own local
LLM and driving it in an **agentic loop** against a live Deephaven session.

- **Left panel** — an AI chat. You type prompts; the assistant's responses and
  the tool calls it makes (running code, searching docs, fetching URLs) stream
  into the conversation.
- **Right panel** — a tabbed view of every table and figure the agent creates.

The agent can:

- **`run_deephaven_code`** — execute Deephaven Python in your session. Created
  tables/figures are shown automatically. Errors are fed back so the model can
  iterate.
- **`search_docs`** — retrieve relevant Deephaven documentation (RAG).
- **`fetch_url`** — pull in external data / reference pages.

> ⚠️ **Experimental.** The agent executes LLM-generated code directly in your
> Deephaven session with no sandboxing. Run it only against trusted local
> servers.

## Prerequisites

1. Install [Ollama](https://ollama.com/) and start it:

   ```sh
   ollama serve
   ```

2. Pull a **tool-calling** chat model and an embedding model (for RAG):

   ```sh
   ollama pull qwen3:30b-a3b
   ollama pull nomic-embed-text
   ```

   Any model tagged `tools` on [ollama.com](https://ollama.com/search?c=tools)
   works. Larger models give better agentic behavior if you have the hardware.

## Install

This is a Python-only package (no JavaScript build step):

```sh
pip install -e plugins/dh-agent
```

It renders through the existing `deephaven.ui` plugin, which must be installed
on the server.

## Usage

In the Deephaven IDE console:

```python
from dh_agent import agent_chat

chat = agent_chat()
```

Then try a prompt such as _"Create a dashboard of World Cup statistics"_.

To let the agent share your console's variables, pass `globals()`:

```python
chat = agent_chat(namespace=globals())
```

## Configuration

Settings are read from the environment (overridable via `AgentConfig`):

| Variable                  | Default                  | Description                              |
| ------------------------- | ------------------------ | ---------------------------------------- |
| `DH_AGENT_OLLAMA_HOST`    | `http://localhost:11434` | Ollama server URL                        |
| `DH_AGENT_MODEL`          | `qwen2.5-coder:7b`       | Chat model (must support tool calling)   |
| `DH_AGENT_EMBED_MODEL`    | `nomic-embed-text`       | Embedding model for RAG                  |
| `DH_AGENT_MAX_ITERATIONS` | `12`                     | Max tool-call rounds per user turn       |
| `DH_AGENT_TEMPERATURE`    | `0.2`                    | Sampling temperature                     |
| `DH_AGENT_RAG_TOP_K`      | `5`                      | Doc chunks retrieved per search          |
| `DH_AGENT_DOCS_PATHS`     | _(empty)_                | `os.pathsep`-separated docs dirs for RAG |

Custom configuration:

```python
from dh_agent import AgentConfig, agent_chat

chat = agent_chat(
    config=AgentConfig(model="llama3.1:8b"),
    docs_paths=["/path/to/deephaven/docs"],
)
```

## How it works

```
user prompt ──► Agent (background thread)
                   │
                   ├─ LLM chat (Ollama) with tool schemas
                   │      │
                   │      ▼ tool_calls
                   ├─ run_deephaven_code ─► CodeExecutor ─► captured tables/figures ─► Output tabs
                   ├─ search_docs ────────► DocIndex (embeddings + cosine)
                   └─ fetch_url ──────────► httpx
                   │
                   ▼ results fed back to the LLM, loop until done
            AgentState (thread-safe) ──► deephaven.ui re-renders chat + output
```

## Roadmap

- Token-by-token streaming into the chat.
- LoRA fine-tuning on Deephaven data (RAG is the current knowledge path).
- Approval / allowlist guardrails for code execution.
