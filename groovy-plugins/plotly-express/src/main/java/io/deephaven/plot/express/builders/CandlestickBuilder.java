package io.deephaven.plot.express.builders;

import java.util.Map;

public final class CandlestickBuilder extends OhlcLikeBuilder {
    public CandlestickBuilder(Object table, Map<String, Object> opts) {
        super(table, opts);
    }

    @Override
    protected String traceType() {
        return "candlestick";
    }
}
