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
        self.assertEqual(names, {"run_deephaven_code", "search_docs", "fetch_url"})

    def test_run_code_returns_text(self):
        result = self.toolbox.dispatch("run_deephaven_code", {"code": "value = 1 + 1"})
        self.assertIn("successfully", result.lower())

    def test_run_code_reports_error(self):
        result = self.toolbox.dispatch("run_deephaven_code", {"code": "boom("})
        self.assertIn("ERROR", result)

    def test_search_docs_uses_callback(self):
        result = self.toolbox.dispatch("search_docs", {"query": "join"})
        self.assertEqual(result, "docs for join")

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
