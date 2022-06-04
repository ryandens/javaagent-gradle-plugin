package io.opentelemetry.javaagent.instrumentation.ryandens.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public enum SampleSingletons {
    INSTANCE;

    public final Tracer tracer;

    SampleSingletons() {
        this.tracer = GlobalOpenTelemetry.get().getTracer("FibonacciTracer");;
    }
}
