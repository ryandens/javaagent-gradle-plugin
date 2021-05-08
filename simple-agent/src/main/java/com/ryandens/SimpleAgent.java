package com.ryandens;

import java.lang.instrument.Instrumentation;

public final class SimpleAgent {

  public static void premain(final String args, final Instrumentation instrumentation) {
    System.out.println("Hello from my simple agent!");
  }
}
