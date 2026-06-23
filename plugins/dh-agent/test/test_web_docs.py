import unittest

from dh_agent import web_docs


class WebDocsTest(unittest.TestCase):
    def test_as_markdown_url_appends_md(self):
        self.assertEqual(
            web_docs._as_markdown_url(
                "https://deephaven.io/core/docs/conceptual/column-types/"
            ),
            "https://deephaven.io/core/docs/conceptual/column-types.md",
        )

    def test_as_markdown_url_keeps_existing_md(self):
        url = "https://deephaven.io/core/docs/conceptual/column-types.md"
        self.assertEqual(web_docs._as_markdown_url(url), url)

    def test_as_markdown_url_strips_fragment_and_query(self):
        self.assertEqual(
            web_docs._as_markdown_url(
                "https://deephaven.io/core/docs/how-to-guides/joins#natural?x=1"
            ),
            "https://deephaven.io/core/docs/how-to-guides/joins.md",
        )

    def test_parse_ddg_urls_filters_to_docs(self):
        html = (
            '<a href="//duckduckgo.com/l/?uddg='
            "https%3A%2F%2Fdeephaven.io%2Fcore%2Fdocs%2Fconceptual%2Fcolumn-types%2F"
            '">x</a>'
            '<a href="//duckduckgo.com/l/?uddg='
            "https%3A%2F%2Fexample.com%2Fother"
            '">y</a>'
        )
        urls = web_docs._parse_ddg_urls(html)
        self.assertEqual(
            urls, ["https://deephaven.io/core/docs/conceptual/column-types/"]
        )

    def test_parse_ddg_urls_dedupes(self):
        enc = "https%3A%2F%2Fdeephaven.io%2Fcore%2Fdocs%2Fa"
        html = f'uddg={enc}&rut=1" ... uddg={enc}&rut=2"'
        self.assertEqual(
            web_docs._parse_ddg_urls(html), ["https://deephaven.io/core/docs/a"]
        )

    def test_search_blank_query_returns_empty(self):
        self.assertEqual(web_docs.search_deephaven_io("   "), "")


if __name__ == "__main__":
    unittest.main()
