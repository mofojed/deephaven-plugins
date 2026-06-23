import unittest

from dh_agent.executor import CodeExecutor
from dh_agent.tools import ToolBox, TOOL_SCHEMAS, _coerce_args


class ToolDispatchTest(unittest.TestCase):
    def setUp(self):
        self.executor = CodeExecutor()
        self.captured = []
        self.toolbox = ToolBox(
            executor=self.executor,
            on_outputs=self.captured.extend,
            doc_search=lambda q: f"docs for {q}",
        )

    def test_schemas_advertise_all_tools(self):
        names = {schema["function"]["name"] for schema in TOOL_SCHEMAS}
        self.assertEqual(
            names,
            {
                "run_deephaven_code",
                "search_docs",
                "read_skill_reference",
                "fetch_url",
            },
        )

    def test_run_code_returns_text(self):
        result = self.toolbox.dispatch("run_deephaven_code", {"code": "value = 1 + 1"})
        self.assertIn("successfully", result.lower())

    def test_run_code_reports_error(self):
        result = self.toolbox.dispatch("run_deephaven_code", {"code": "boom("})
        self.assertIn("ERROR", result)

    def test_search_docs_uses_callback(self):
        result = self.toolbox.dispatch("search_docs", {"query": "join"})
        self.assertEqual(result, "docs for join")

    def test_search_docs_advertised_when_available(self):
        names = {s["function"]["name"] for s in self.toolbox.schemas}
        self.assertIn("search_docs", names)

    def test_search_docs_hidden_when_unavailable(self):
        toolbox = ToolBox(executor=self.executor, doc_search=None)
        names = {s["function"]["name"] for s in toolbox.schemas}
        self.assertNotIn("search_docs", names)
        self.assertIn("run_deephaven_code", names)

    def test_read_skill_reference_returns_content(self):
        result = self.toolbox.dispatch("read_skill_reference", {"name": "joins"})
        self.assertNotIn("Unknown reference", result)
        self.assertTrue(result.strip())

    def test_read_skill_reference_requires_name(self):
        result = self.toolbox.dispatch("read_skill_reference", {"name": ""})
        self.assertIn("Provide a reference name", result)

    def test_fetch_url_rejects_non_http(self):
        result = self.toolbox.dispatch("fetch_url", {"url": "file:///etc/passwd"})
        self.assertIn("absolute http", result)

    def test_unknown_tool(self):
        self.assertIn("Unknown tool", self.toolbox.dispatch("nope", {}))

    def test_coerce_args_parses_json_string(self):
        self.assertEqual(_coerce_args('{"a": 1}'), {"a": 1})
        self.assertEqual(_coerce_args("not json"), {})
        self.assertEqual(_coerce_args({"a": 1}), {"a": 1})


if __name__ == "__main__":
    unittest.main()
