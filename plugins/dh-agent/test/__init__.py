from deephaven_server.server import Server

# Create a Server instance to initialize the JVM before any test module imports
# anything from the `deephaven` namespace. Importing `deephaven` triggers a JVM
# readiness check, so the instance must exist first. We don't need to start it.
if Server.instance is None:
    Server(port=10075, jvm_args=["-Xmx2g"])
