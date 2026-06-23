import unittest
from dataclasses import dataclass

from dh_agent.docs import make_doc_search, _NO_RESULTS


@dataclass
class _Chunk:
    source: str
    text: str


class _FakeIndex:
    def __init__(self, scored):
        self._scored = scored
        self.build_calls = 0

    def build(self):
        self.build_calls += 1

    def search_scored(self, query, top_k=5):
        return self._scored


class DocSearchTest(unittest.TestCase):
    def test_strong_local_match_skips_web(self):
        index = _FakeIndex([(_Chunk("joins.md", "natural join"), 0.8)])
        calls = []
        search = make_doc_search(
            index,
            min_score=0.62,
            web_searcher=lambda q: calls.append(q) or "WEB",
        )
        result = search("how to join")
        self.assertIn("[joins.md]", result)
        self.assertIn("natural join", result)
        self.assertEqual(calls, [])

    def test_weak_local_match_falls_back_to_web(self):
        index = _FakeIndex([(_Chunk("iceberg.md", "tangential"), 0.40)])
        search = make_doc_search(
            index,
            min_score=0.62,
            web_searcher=lambda q: "From deephaven.io ...",
        )
        self.assertEqual(search("column types"), "From deephaven.io ...")

    def test_weak_local_used_when_web_empty(self):
        index = _FakeIndex([(_Chunk("csv.md", "closest local"), 0.50)])
        search = make_doc_search(index, min_score=0.62, web_searcher=lambda q: "")
        result = search("read parquet")
        self.assertIn("closest local", result)

    def test_no_index_uses_web(self):
        search = make_doc_search(None, web_searcher=lambda q: "WEB RESULT")
        self.assertEqual(search("anything"), "WEB RESULT")

    def test_no_index_no_web_returns_none(self):
        self.assertIsNone(make_doc_search(None, web_fallback=False))

    def test_no_results_message(self):
        index = _FakeIndex([])
        search = make_doc_search(index, web_fallback=False)
        self.assertEqual(search("x"), _NO_RESULTS)

    def test_index_built_once(self):
        index = _FakeIndex([(_Chunk("ui.md", "comp"), 0.9)])
        search = make_doc_search(index, web_searcher=lambda q: "")
        search("a")
        search("b")
        self.assertEqual(index.build_calls, 1)


if __name__ == "__main__":
    unittest.main()
