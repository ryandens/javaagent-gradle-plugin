package io.opentelemetry.javaagent.instrumentation.ryandens;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public enum Singletons {
    INSTANCE;

    public final Tracer tracer;

    Singletons() {
        tracer = GlobalOpenTelemetry.get().getTracer("FibonacciTracer");
    }
}
