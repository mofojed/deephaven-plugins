package io.deephaven.ui.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Minimal JSON-RPC 2.0 dispatcher matching the Python plugin's wire format byte-for-byte. The
 * protocol surface is tiny ({@code setState}, {@code setUrlState}, {@code callCallable},
 * {@code closeCallable}); pulling in a full library would dwarf the implementation.
 *
 * <p>Methods take a list of params and return either a result object (sent back to the client for
 * requests with an {@code id}) or {@code null} for notifications.
 */
public final class JsonRpcDispatcher {

    @FunctionalInterface
    public interface Method {
        Object invoke(List<Object> params);
    }

    private final ObjectMapper mapper;
    private final Map<String, Method> methods = new HashMap<>();

    public JsonRpcDispatcher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(String name, Method method) {
        methods.put(name, method);
    }

    /** Handle a single JSON-RPC request payload. Returns the response JSON (or {@code null} for notifications). */
    public String handle(String payload) {
        JsonNode node;
        try {
            node = mapper.readTree(payload);
        } catch (Exception e) {
            return errorResponse(null, -32700, "Parse error");
        }
        if (node == null || !node.isObject()) {
            return errorResponse(null, -32600, "Invalid request");
        }
        JsonNode idNode = node.get("id");
        boolean isNotification = idNode == null || idNode.isNull();

        String method = node.path("method").asText(null);
        if (method == null) {
            return isNotification ? null : errorResponse(idNode, -32600, "Invalid request: missing method");
        }
        Method handler = methods.get(method);
        if (handler == null) {
            return isNotification ? null : errorResponse(idNode, -32601, "Method not found: " + method);
        }

        List<Object> params = paramsAsList(node.get("params"));
        Object result;
        try {
            result = handler.invoke(params);
        } catch (RuntimeException e) {
            return isNotification ? null : errorResponse(idNode, -32603, e.getMessage());
        }
        if (isNotification) {
            return null;
        }
        return successResponse(idNode, result);
    }

    /** Build a notification (no id) — Python's {@code _make_notification}. */
    public String notification(String method, Object... params) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("jsonrpc", "2.0");
        out.put("method", method);
        ArrayNode arr = out.putArray("params");
        for (Object p : params) {
            arr.add(mapper.valueToTree(p));
        }
        try {
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON-RPC notification", e);
        }
    }

    private String successResponse(JsonNode id, Object result) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("jsonrpc", "2.0");
        out.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
        out.set("result", mapper.valueToTree(result));
        try {
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON-RPC response", e);
        }
    }

    private String errorResponse(JsonNode id, int code, String message) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("jsonrpc", "2.0");
        out.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
        ObjectNode error = out.putObject("error");
        error.put("code", code);
        error.put("message", message == null ? "" : message);
        try {
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"\"}}";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> paramsAsList(JsonNode params) {
        if (params == null || params.isNull()) {
            return List.of();
        }
        if (params.isArray()) {
            return (List<Object>) mapper.convertValue(params, List.class);
        }
        return List.of(mapper.convertValue(params, Object.class));
    }

    // Useful for tests / consumers that want to map params through a function.
    public <T> T convertParam(Object param, Class<T> type) {
        return mapper.convertValue(param, type);
    }

    public <T> T convertParam(Object param, Function<JsonNode, T> mapper) {
        return mapper.apply(this.mapper.valueToTree(param));
    }
}
