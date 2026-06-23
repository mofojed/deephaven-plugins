import unittest

import numpy as np

from dh_agent.rag import _chunk_text, _normalize, _CHUNK_CHARS


class RagTest(unittest.TestCase):
    def test_chunk_short_text(self):
        chunks = _chunk_text("hello")
        self.assertEqual(chunks, ["hello"])

    def test_chunk_long_text_overlaps(self):
        text = "x" * (_CHUNK_CHARS * 2)
        chunks = _chunk_text(text)
        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(c) <= _CHUNK_CHARS for c in chunks))

    def test_normalize_unit_vectors(self):
        matrix = np.array([[3.0, 4.0], [0.0, 0.0]], dtype=np.float32)
        normalized = _normalize(matrix)
        self.assertAlmostEqual(float(np.linalg.norm(normalized[0])), 1.0, places=5)
        # Zero vector stays zero (no divide-by-zero).
        self.assertAlmostEqual(float(np.linalg.norm(normalized[1])), 0.0, places=5)


if __name__ == "__main__":
    unittest.main()
