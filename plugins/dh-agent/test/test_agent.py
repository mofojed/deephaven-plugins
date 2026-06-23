import unittest

from dh_agent.agent import (
    Agent,
    AgentState,
    ChatMessage,
    _extract_python_code,
    _extract_tool_calls_from_text,
)
from dh_agent.config import AgentConfig
from dh_agent.executor import CapturedOutput, CodeExecutor
from dh_agent.tools import ToolBox


class StubClient:
    """A fake Ollama client that returns scripted messages."""

    def __init__(self, responses):
        self._responses = list(responses)
        self.calls = []

    def chat(self, messages, tools=None):
        self.calls.append(list(messages))
        return self._responses.pop(0)


class AgentStateTest(unittest.TestCase):
    def test_subscribe_and_notify(self):
        state = AgentState()
        events = []
        unsubscribe = state.subscribe(lambda: events.append(1))

        state.add_message(ChatMessage(role="user", content="hi"))
        self.assertEqual(len(events), 1)
        self.assertEqual(len(state.messages), 1)

        unsubscribe()
        state.add_message(ChatMessage(role="user", content="again"))
        self.assertEqual(len(events), 1)

    def test_add_outputs_assigns_keys_and_titles(self):
        state = AgentState()
        state.add_outputs(
            [
                CapturedOutput(name="t1", value=object(), kind="table"),
                CapturedOutput(name="fig", value=object(), kind="figure"),
            ]
        )
        outputs = state.outputs
        self.assertEqual([o.title for o in outputs], ["t1", "fig"])
        self.assertEqual(len({o.key for o in outputs}), 2)

    def test_busy_flag(self):
        state = AgentState()
        self.assertFalse(state.busy)
        state.set_busy(True)
        self.assertTrue(state.busy)


class AgentLoopTest(unittest.TestCase):
    def _make_agent(self, responses):
        state = AgentState()
        client = StubClient(responses)
        toolbox = ToolBox(executor=CodeExecutor(), on_outputs=state.add_outputs)
        agent = Agent(state=state, client=client, toolbox=toolbox, config=AgentConfig())
        return agent, state, client

    def test_simple_response_without_tools(self):
        agent, state, client = self._make_agent(
            [{"content": "Hello!", "tool_calls": []}]
        )
        agent._run_turn("hi there")

        roles = [m.role for m in state.messages]
        self.assertEqual(roles, ["user", "assistant"])
        self.assertEqual(state.messages[-1].content, "Hello!")
        self.assertFalse(state.busy)

    def test_tool_call_then_final_answer(self):
        responses = [
            {
                "content": "",
                "tool_calls": [
                    {
                        "function": {
                            "name": "run_deephaven_code",
                            "arguments": {"code": "note = 'done'"},
                        }
                    }
                ],
            },
            {"content": "All set.", "tool_calls": []},
        ]
        agent, state, client = self._make_agent(responses)
        agent._run_turn("do a thing")

        roles = [m.role for m in state.messages]
        # user, tool(call), tool(result), assistant
        self.assertEqual(roles, ["user", "tool", "tool", "assistant"])
        self.assertEqual(len(client.calls), 2)
        self.assertEqual(state.messages[-1].content, "All set.")

    def test_loop_respects_max_iterations(self):
        # Always returns a tool call; loop must terminate at max_iterations.
        forever = [
            {
                "content": "",
                "tool_calls": [
                    {
                        "function": {
                            "name": "run_deephaven_code",
                            "arguments": {"code": "x = 1"},
                        }
                    }
                ],
            }
        ] * 50
        state = AgentState()
        client = StubClient(forever)
        toolbox = ToolBox(executor=CodeExecutor())
        agent = Agent(
            state=state,
            client=client,
            toolbox=toolbox,
            config=AgentConfig(max_iterations=3),
        )
        agent._run_turn("loop")
        self.assertEqual(len(client.calls), 3)


class FallbackExtractionTest(unittest.TestCase):
    def test_extract_python_code_blocks(self):
        content = "Here is the code:\n```python\nt = 1 + 1\n```\nAnd done."
        self.assertEqual(_extract_python_code(content), ["t = 1 + 1"])

    def test_extract_python_code_skips_non_python(self):
        content = "Run:\n```bash\nls -la\n```"
        self.assertEqual(_extract_python_code(content), [])

    def test_extract_untagged_code_block(self):
        content = "```\nt = 2\n```"
        self.assertEqual(_extract_python_code(content), ["t = 2"])

    def test_extract_json_tool_call_fenced(self):
        content = (
            'Sure:\n```json\n{"name": "run_deephaven_code", '
            '"arguments": {"code": "x = 1"}}\n```'
        )
        calls = _extract_tool_calls_from_text(content)
        self.assertEqual(len(calls), 1)
        name, args = calls[0]
        self.assertEqual(name, "run_deephaven_code")
        self.assertEqual(args, {"code": "x = 1"})

    def test_extract_json_tool_call_function_wrapper(self):
        content = (
            '{"function": {"name": "search_docs", ' '"arguments": {"query": "join"}}}'
        )
        calls = _extract_tool_calls_from_text(content)
        self.assertEqual(calls, [("search_docs", {"query": "join"})])

    def test_unknown_tool_name_ignored(self):
        content = '{"name": "rm_rf", "arguments": {"path": "/"}}'
        self.assertEqual(_extract_tool_calls_from_text(content), [])


class FallbackLoopTest(unittest.TestCase):
    def _make_agent(self, responses):
        state = AgentState()
        client = StubClient(responses)
        toolbox = ToolBox(executor=CodeExecutor(), on_outputs=state.add_outputs)
        agent = Agent(state=state, client=client, toolbox=toolbox, config=AgentConfig())
        return agent, state, client

    def test_markdown_code_block_is_executed(self):
        responses = [
            {"content": "```python\nnote = 'hi'\n```", "tool_calls": []},
            {"content": "Done.", "tool_calls": []},
        ]
        agent, state, client = self._make_agent(responses)
        agent._run_turn("make a note")

        roles = [m.role for m in state.messages]
        self.assertEqual(roles, ["user", "assistant", "tool", "tool", "assistant"])
        self.assertEqual(agent._toolbox._executor.namespace.get("note"), "hi")
        self.assertEqual(len(client.calls), 2)

    def test_json_tool_call_in_content_is_dispatched(self):
        responses = [
            {
                "content": '```json\n{"name": "run_deephaven_code", '
                '"arguments": {"code": "answer = 42"}}\n```',
                "tool_calls": [],
            },
            {"content": "All set.", "tool_calls": []},
        ]
        agent, state, client = self._make_agent(responses)
        agent._run_turn("compute")

        self.assertEqual(agent._toolbox._executor.namespace.get("answer"), 42)
        self.assertEqual(state.messages[-1].content, "All set.")


class CancellationTest(unittest.TestCase):
    def _make_agent(self, responses):
        state = AgentState()
        client = StubClient(responses)
        toolbox = ToolBox(executor=CodeExecutor(), on_outputs=state.add_outputs)
        agent = Agent(state=state, client=client, toolbox=toolbox, config=AgentConfig())
        return agent, state, client

    def test_cancel_sets_event_only_when_busy(self):
        agent, state, _ = self._make_agent([])
        agent.cancel()
        self.assertFalse(agent._cancel_event.is_set())
        state.set_busy(True)
        agent.cancel()
        self.assertTrue(agent._cancel_event.is_set())

    def test_cancel_before_turn_stops_immediately(self):
        agent, state, client = self._make_agent([{"content": "hi", "tool_calls": []}])
        agent._cancel_event.set()
        agent._run_turn("hello")

        roles = [m.role for m in state.messages]
        self.assertEqual(len(client.calls), 0)
        self.assertIn("status", roles)
        self.assertNotIn("assistant", roles)
        self.assertFalse(state.busy)

    def test_cancel_during_model_call_skips_tool_execution(self):
        holder: dict = {}

        class CancellingClient:
            def __init__(self):
                self.calls = []

            def chat(self, messages, tools=None):
                self.calls.append(list(messages))
                holder["agent"]._cancel_event.set()
                return {
                    "content": "",
                    "tool_calls": [
                        {
                            "function": {
                                "name": "run_deephaven_code",
                                "arguments": {"code": "leaked = 1"},
                            }
                        }
                    ],
                }

        state = AgentState()
        executor = CodeExecutor()
        client = CancellingClient()
        toolbox = ToolBox(executor=executor)
        agent = Agent(state=state, client=client, toolbox=toolbox, config=AgentConfig())
        holder["agent"] = agent

        agent._run_turn("go")

        self.assertEqual(len(client.calls), 1)
        roles = [m.role for m in state.messages]
        self.assertNotIn("tool", roles)
        self.assertIn("status", roles)
        self.assertNotIn("leaked", executor.namespace)
        self.assertFalse(state.busy)


if __name__ == "__main__":
    unittest.main()
