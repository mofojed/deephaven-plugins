"""Demo script for the Deephaven LLM agent.

Run this inside a Deephaven Python console (or a server-backed session) after
starting Ollama (`ollama serve`) and pulling a tool-capable model.

    ollama pull qwen3-coder:30b
    # Add any other models you want, though qwen3 seems to work well
    # ollama pull qwen2.5-coder:7b
    ollama pull nomic-embed-text

Then in the console:

    exec(open("plugins/dh-agent/examples/demo.py").read())

and try a prompt like "Create a table of the first 100 fibonacci numbers".
"""

from dh_agent import agent_chat

# Share the console globals so the agent's variables show up in your session.
chat = agent_chat(namespace=globals())
