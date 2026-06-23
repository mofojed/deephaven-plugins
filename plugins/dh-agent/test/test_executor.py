import unittest
import threading

from .BaseTest import ServerTestCase

from dh_agent.executor import CodeExecutor


class ExecutorTest(ServerTestCase):
    def test_captures_created_table(self):
        executor = CodeExecutor()
        result = executor.execute("my_table = empty_table(10)")

        self.assertTrue(result.ok, result.error)
        self.assertEqual(len(result.outputs), 1)
        output = result.outputs[0]
        self.assertEqual(output.name, "my_table")
        self.assertEqual(output.kind, "table")

    def test_executes_table_ops_on_background_thread(self):
        # The agent dispatches code from a background thread, which has no
        # ExecutionContext of its own. The executor must re-apply the context
        # captured at construction time, or table ops fail with
        # ExecutionContextRegistrationException.
        executor = CodeExecutor()
        box: dict[str, object] = {}

        def run() -> None:
            box["result"] = executor.execute("bg = empty_table(3).update(['X = i'])")

        thread = threading.Thread(target=run, daemon=True)
        thread.start()
        thread.join()

        result = box["result"]
        self.assertTrue(result.ok, result.error)
        self.assertIn("bg", {o.name for o in result.outputs})

    def test_persists_namespace_between_calls(self):
        executor = CodeExecutor()
        executor.execute("base = empty_table(5)")
        result = executor.execute("derived = base.update(['X = i'])")

        self.assertTrue(result.ok, result.error)
        names = {o.name for o in result.outputs}
        self.assertIn("derived", names)

    def test_captures_stdout(self):
        executor = CodeExecutor()
        result = executor.execute("print('hello world')")

        self.assertTrue(result.ok, result.error)
        self.assertIn("hello world", result.stdout)
        self.assertEqual(result.outputs, [])

    def test_reports_errors(self):
        executor = CodeExecutor()
        result = executor.execute("1 / 0")

        self.assertFalse(result.ok)
        self.assertIn("ZeroDivisionError", result.error)
        model_text = result.to_model_text()
        self.assertIn("ERROR", model_text)


if __name__ == "__main__":
    unittest.main()
