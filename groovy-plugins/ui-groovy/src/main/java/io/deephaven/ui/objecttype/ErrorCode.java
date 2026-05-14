package io.deephaven.ui.objecttype;

/** Error codes echoed to the client in {@code documentError} notifications. Mirrors Python. */
public enum ErrorCode {
    DOCUMENT_ERROR("DOCUMENT_ERROR");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
