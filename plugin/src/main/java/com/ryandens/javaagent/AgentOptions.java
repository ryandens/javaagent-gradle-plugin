package com.ryandens.javaagent;

import org.gradle.api.attributes.Attribute;

/** Dependency attributes used to retain agent options through copying and resolution. */
public final class AgentOptions {
  /** The option string owned by an individual dependency declaration. */
  public static final Attribute<String> OPTIONS =
      Attribute.of("com.ryandens.javaagent.options", String.class);

  private AgentOptions() {}
}
