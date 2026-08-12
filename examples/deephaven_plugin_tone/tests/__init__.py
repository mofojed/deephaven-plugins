from deephaven_server.server import Server

# Create a Server instance to initialize the JVM before importing anything from
# the deephaven namespace. The server does not need to be started.
if Server.instance is None:
    Server(port=11000, jvm_args=["-Xmx4g"])
