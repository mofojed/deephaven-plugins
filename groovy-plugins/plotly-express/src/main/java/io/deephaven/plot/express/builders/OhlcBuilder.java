package io.deephaven.plot.express.builders;

import java.util.Map;

public final class OhlcBuilder extends OhlcLikeBuilder {
    public OhlcBuilder(Object table, Map<String, Object> opts) {
        super(table, opts);
    }

    @Override
    protected String traceType() {
        return "ohlc";
    }
}
