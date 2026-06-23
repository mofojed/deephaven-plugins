import unittest

from dh_agent.agent import Agent, AgentState, ChatMessage
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


if __name__ == "__main__":
    unittest.main()
