package com.ryandens.javaagent;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;

/** Dependency attributes used to retain agent options through copying and resolution. */
public final class AgentOptions {
  /** The option string owned by an individual dependency declaration. */
  public static final Attribute<String> OPTIONS =
      Attribute.of("com.ryandens.javaagent.options", String.class);

  static final Attribute<String> DECLARED_ON =
      Attribute.of("com.ryandens.javaagent.declared-on", String.class);

  private AgentOptions() {}

  /** Records declaration scope so direct options take precedence over inherited options. */
  public static void trackDeclarations(final Configuration configuration) {
    final String name = configuration.getName();
    configuration
        .getDependencies()
        .withType(ModuleDependency.class)
        .all(
            dependency ->
                dependency.attributes(attributes -> attributes.attribute(DECLARED_ON, name)));
  }
}
