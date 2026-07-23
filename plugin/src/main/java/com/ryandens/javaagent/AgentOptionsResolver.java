package com.ryandens.javaagent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.Provider;

/**
 * Resolves the {@link JavaagentExtension#getAgentOptions() agent options} declared by coordinate
 * into a map keyed by the resolved artifact's full canonical file path, which is the join key every
 * {@code -javaagent:} argument site can compute (the run/test sites hold the {@link File}, the
 * distribution and jib sites use {@code file.getCanonicalPath()}).
 *
 * <p>This class is written in Java for the same reason as {@link JavaForkOptionsConfigurer}: to
 * avoid the <a
 * href="https://docs.gradle.org/7.5.1/userguide/validation_problems.html#implementation_unknown">Gradle
 * task input serialization limitation</a> with Kotlin lambdas.
 */
public final class AgentOptionsResolver {

  /** Prevent instantiation for utility class. */
  private AgentOptionsResolver() {}

  /**
   * Builds a {@code filePath -> options} map from the resolved artifacts of the provided
   * configuration and the coordinate-keyed options declared on the {@link JavaagentExtension}. Only
   * agents that have options declared appear in the resulting map.
   *
   * <p>The returned {@link Provider} is lazy and configuration-cache safe: it reads the
   * configuration's resolved artifacts and the options map without touching {@code Project} at
   * execution time.
   *
   * @param configuration the resolvable javaagent configuration whose artifacts should be matched
   * @param coordinateOptions options keyed by dependency coordinate (see {@link
   *     JavaagentExtension})
   * @return provider of a map from resolved artifact file path to its option string
   */
  public static Provider<Map<String, String>> optionsByFilePath(
      final Configuration configuration, final Provider<Map<String, String>> coordinateOptions) {
    return configuration
        .getIncoming()
        .getArtifacts()
        .getResolvedArtifacts()
        .map(
            artifacts -> {
              final Map<String, String> coordinates = coordinateOptions.getOrElse(new HashMap<>());
              final Map<String, String> byFilePath = new HashMap<>();
              if (coordinates.isEmpty()) {
                return byFilePath;
              }
              for (final ResolvedArtifactResult artifact : artifacts) {
                final String key = coordinateKey(artifact.getId().getComponentIdentifier());
                final String options = coordinates.get(key);
                if (options != null) {
                  try {
                    byFilePath.put(artifact.getFile().getCanonicalPath(), options);
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                }
              }
              return byFilePath;
            });
  }

  /**
   * Derives the coordinate key used to look up options for a resolved artifact. Module dependencies
   * are keyed by {@code group:name} (version intentionally excluded so options survive version
   * bumps), project dependencies by their project path, and anything else by its display name as a
   * best-effort fallback.
   */
  private static String coordinateKey(final ComponentIdentifier identifier) {
    if (identifier instanceof ModuleComponentIdentifier) {
      final ModuleComponentIdentifier module = (ModuleComponentIdentifier) identifier;
      return module.getGroup() + ":" + module.getModule();
    }
    if (identifier instanceof ProjectComponentIdentifier) {
      return ((ProjectComponentIdentifier) identifier).getProjectPath();
    }
    return identifier.getDisplayName();
  }
}
