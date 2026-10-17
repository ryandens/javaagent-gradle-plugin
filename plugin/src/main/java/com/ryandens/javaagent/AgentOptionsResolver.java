package com.ryandens.javaagent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.Provider;

/**
 * Resolves options owned by dependencies in a plugin-specific configuration. Returns a map keyed by
 * the resolved artifact's full canonical file path, which is the join key every {@code -javaagent:}
 * argument site can compute (the run/test sites hold the {@link File}, the distribution and jib
 * sites use {@code file.getCanonicalPath()}).
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
   * configuration. Direct declarations override inherited declarations. Only agents that have
   * options declared appear in the resulting map.
   *
   * <p>The returned {@link Provider} is lazy and configuration-cache safe: it reads the
   * configuration's resolved artifacts and the options map without touching {@code Project} at
   * execution time.
   *
   * @param configuration the resolvable javaagent configuration whose artifacts should be matched
   * @return provider of a map from resolved artifact file path to its option string
   */
  public static Provider<Map<String, String>> optionsByFilePath(final Configuration configuration) {
    final String configurationName = configuration.getName();
    return configuration
        .getIncoming()
        .getResolutionResult()
        .getRootComponent()
        .map(root -> dependencyOptions(root, configurationName))
        .zip(
            configuration.getIncoming().getArtifacts().getResolvedArtifacts(),
            (dependencyOptions, artifacts) -> {
              final Map<String, String> byFilePath = new HashMap<>();
              for (final ResolvedArtifactResult artifact : artifacts) {
                final ComponentIdentifier component = artifact.getId().getComponentIdentifier();
                final String options = dependencyOptions.get(component);
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

  private static Map<ComponentIdentifier, String> dependencyOptions(
      final ResolvedComponentResult root, final String configurationName) {
    final Map<ComponentIdentifier, String> options = new HashMap<>();
    final Map<ComponentIdentifier, Integer> priorities = new HashMap<>();
    for (final DependencyResult dependency : root.getDependencies()) {
      if (!(dependency instanceof ResolvedDependencyResult)) {
        continue;
      }
      final String value =
          dependency.getRequested().getAttributes().getAttribute(AgentOptions.OPTIONS);
      if (value == null) {
        continue;
      }
      final ComponentIdentifier component =
          ((ResolvedDependencyResult) dependency).getSelected().getId();
      final String declaredOn =
          dependency.getRequested().getAttributes().getAttribute(AgentOptions.DECLARED_ON);
      final int priority = configurationName.equals(declaredOn) ? 1 : 0;
      final int previousPriority = priorities.getOrDefault(component, -1);
      if (priority > previousPriority) {
        options.put(component, value);
        priorities.put(component, priority);
      } else if (priority == previousPriority && !value.equals(options.get(component))) {
        throw new GradleException(
            "Conflicting javaagent options for "
                + component.getDisplayName()
                + " in configuration '"
                + configurationName
                + "'. Configure one option string per agent.");
      }
    }
    return options;
  }
}
