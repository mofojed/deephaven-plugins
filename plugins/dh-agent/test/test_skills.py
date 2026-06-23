import unittest

from dh_agent import skills


class SkillsTest(unittest.TestCase):
    def test_load_skill_body_is_nonempty(self):
        body = skills.load_skill_body()
        self.assertTrue(body.strip())

    def test_load_skill_body_strips_frontmatter(self):
        body = skills.load_skill_body()
        self.assertFalse(body.lstrip().startswith("---"))

    def test_list_references_includes_expected_topics(self):
        names = set(skills.list_references())
        self.assertIn("joins", names)
        self.assertIn("aggregations", names)
        self.assertIn("plotting", names)

    def test_read_reference_returns_content(self):
        content = skills.read_reference("joins")
        self.assertTrue(content.strip())
        self.assertNotIn("Unknown reference", content)

    def test_read_reference_accepts_md_extension(self):
        content = skills.read_reference("joins.md")
        self.assertTrue(content.strip())
        self.assertNotIn("Unknown reference", content)

    def test_read_reference_unknown_returns_message(self):
        content = skills.read_reference("does-not-exist")
        self.assertIn("Unknown reference", content)

    def test_read_reference_rejects_traversal(self):
        content = skills.read_reference("../SKILL")
        self.assertIn("Unknown reference", content)

    def test_strip_frontmatter_without_block(self):
        text = "no frontmatter here"
        self.assertEqual(skills._strip_frontmatter(text), text)

    def test_skill_doc_paths_points_at_references(self):
        paths = skills.skill_doc_paths()
        self.assertEqual(len(paths), 1)
        self.assertTrue(paths[0].endswith("references"))


if __name__ == "__main__":
    unittest.main()
