import unittest


class ServerTestCase(unittest.TestCase):
    """Base test case for tests that use the Deephaven server.

    The server/JVM is initialized once in ``test/__init__.py`` before any test
    module is imported, so this is just a plain ``TestCase`` alias kept for
    readability at the call sites.
    """


if __name__ == "__main__":
    unittest.main()
