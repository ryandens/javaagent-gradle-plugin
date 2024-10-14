package com.ryandens.example;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.Collections;
import java.util.List;

@AutoService(InstrumentationModule.class)
public final class SampleInstrumentationModule extends InstrumentationModule {
  public SampleInstrumentationModule() {
    super("sample");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new SampleInstrumentation());
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.contains("ryandens");
  }
}
