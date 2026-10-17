package com.ryandens.javaagent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentSelector;
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
   * configuration. Declarations in descendant configurations override ancestor declarations. Only
   * agents that have options declared appear in the resulting map.
   *
   * <p>The returned {@link Provider} is lazy and configuration-cache safe: it reads the
   * configuration's resolved artifacts and the options map without touching {@code Project} at
   * execution time.
   *
   * @param configurationProvider the resolvable javaagent configuration whose artifacts should be
   *     matched
   * @return provider of a map from resolved artifact file path to its option string
   */
  public static Provider<Map<String, String>> optionsByFilePath(
      final Provider<? extends Configuration> configurationProvider) {
    return configurationProvider.map(
        configuration -> {
          // Resolve first: callbacks may add direct declarations that override inherited options.
          final ResolvedComponentResult root =
              configuration.getIncoming().getResolutionResult().getRootComponent().get();
          final Map<ComponentIdentifier, String> options = dependencyOptions(root, configuration);
          final Map<String, String> byFilePath = new HashMap<>();
          if (options.isEmpty()) {
            return byFilePath;
          }
          final Map<String, Set<String>> requests = new HashMap<>();
          for (final DependencyResult dependency : root.getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult) {
              final String value =
                  dependency.getRequested().getAttributes().getAttribute(AgentOptions.OPTIONS);
              final ComponentIdentifier selected =
                  ((ResolvedDependencyResult) dependency).getSelected().getId();
              if (value != null && value.equals(options.get(selected))) {
                requests
                    .computeIfAbsent(
                        requestedKey(dependency.getRequested()), unused -> new HashSet<>())
                    .add(value);
              }
            }
          }
          // Resolving just the declarations that own options identifies their actual artifacts.
          // Component-wide artifact views also include optionless classifier/alternate requests.
          final Configuration optionConfiguration =
              configuration.copyRecursive(
                  dependency -> {
                    if (!(dependency instanceof ModuleDependency)) {
                      return false;
                    }
                    final ModuleDependency module = (ModuleDependency) dependency;
                    final Set<String> values = requests.get(dependencyKey(module));
                    return values != null
                        && values.contains(
                            module.getAttributes().getAttribute(AgentOptions.OPTIONS));
                  });
          preserveSelections(configuration, optionConfiguration, root);
          // The original configuration already validated locks; the unnamed copy uses those
          // selections.
          optionConfiguration.getResolutionStrategy().deactivateDependencyLocking();
          final Map<String, ComponentIdentifier> originalArtifacts = new HashMap<>();
          for (final ResolvedArtifactResult artifact :
              configuration.getIncoming().getArtifacts().getArtifacts()) {
            originalArtifacts.put(
                canonicalPath(artifact), artifact.getId().getComponentIdentifier());
          }
          for (final ResolvedArtifactResult artifact :
              optionConfiguration.getIncoming().getArtifacts().getArtifacts()) {
            final String path = canonicalPath(artifact);
            final ComponentIdentifier component = artifact.getId().getComponentIdentifier();
            final String value = options.get(component);
            if (value == null || !component.equals(originalArtifacts.get(path))) {
              throw new GradleException(
                  "Agent option resolution changed the selected artifact: "
                      + artifact.getId().getDisplayName());
            }
            byFilePath.put(path, value);
          }
          return byFilePath;
        });
  }

  private static String canonicalPath(final ResolvedArtifactResult artifact) {
    try {
      return artifact.getFile().getCanonicalPath();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void preserveSelections(
      final Configuration original, final Configuration copy, final ResolvedComponentResult root) {
    final Map<String, ComponentIdentifier> selections = new HashMap<>();
    for (final DependencyResult dependency :
        original.getIncoming().getResolutionResult().getAllDependencies()) {
      if (dependency instanceof ResolvedDependencyResult) {
        selections.put(
            requestedKey(dependency.getRequested()),
            ((ResolvedDependencyResult) dependency).getSelected().getId());
      }
    }
    final ProjectComponentIdentifier rootId = (ProjectComponentIdentifier) root.getId();
    copy.getResolutionStrategy()
        .dependencySubstitution(
            substitutions ->
                substitutions.all(
                    details -> {
                      final ComponentIdentifier selected =
                          selections.get(requestedKey(details.getRequested()));
                      if (selected instanceof ModuleComponentIdentifier) {
                        final ModuleComponentIdentifier module =
                            (ModuleComponentIdentifier) selected;
                        details.useTarget(
                            substitutions.module(
                                module.getGroup()
                                    + ":"
                                    + module.getModule()
                                    + ":"
                                    + module.getVersion()));
                      } else if (selected instanceof ProjectComponentIdentifier) {
                        final ProjectComponentIdentifier project =
                            (ProjectComponentIdentifier) selected;
                        if (project.getBuild().equals(rootId.getBuild())) {
                          details.useTarget(substitutions.project(project.getProjectPath()));
                        }
                      }
                    }));
  }

  private static String dependencyKey(final ModuleDependency dependency) {
    return dependency instanceof ProjectDependency
        ? "project:" + ((ProjectDependency) dependency).getPath()
        : "module:" + dependency.getGroup() + ":" + dependency.getName();
  }

  private static Map<String, Map<Configuration, Set<String>>> declaredOptions(
      final Configuration configuration) {
    final Map<String, Map<Configuration, Set<String>>> options = new HashMap<>();
    for (final Configuration scope : configuration.getHierarchy()) {
      for (final ModuleDependency dependency :
          scope.getDependencies().withType(ModuleDependency.class)) {
        final String value = dependency.getAttributes().getAttribute(AgentOptions.OPTIONS);
        if (value != null) {
          options
              .computeIfAbsent(dependencyKey(dependency), unused -> new HashMap<>())
              .computeIfAbsent(scope, unused -> new HashSet<>())
              .add(value);
        }
      }
    }
    return options;
  }

  private static String requestedKey(final ComponentSelector selector) {
    if (selector instanceof ProjectComponentSelector) {
      return "project:" + ((ProjectComponentSelector) selector).getProjectPath();
    }
    if (selector instanceof ModuleComponentSelector) {
      final ModuleComponentSelector module = (ModuleComponentSelector) selector;
      return "module:" + module.getGroup() + ":" + module.getModule();
    }
    return selector.getDisplayName();
  }

  private static Map<ComponentIdentifier, String> dependencyOptions(
      final ResolvedComponentResult root, final Configuration configuration) {
    final Map<String, Map<Configuration, Set<String>>> declarations =
        declaredOptions(configuration);
    final Map<ComponentIdentifier, Map<Configuration, Set<String>>> candidates = new HashMap<>();
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
      final Map<Configuration, Set<String>> scopes =
          declarations.get(requestedKey(dependency.getRequested()));
      if (scopes != null) {
        scopes.forEach(
            (scope, values) -> {
              if (values.contains(value)) {
                candidates
                    .computeIfAbsent(component, unused -> new HashMap<>())
                    .computeIfAbsent(scope, unused -> new HashSet<>())
                    .add(value);
              }
            });
      }
    }
    final Map<ComponentIdentifier, String> options = new HashMap<>();
    candidates.forEach(
        (component, scopes) -> {
          final Set<String> values = new HashSet<>();
          scopes.forEach(
              (scope, declaredValues) -> {
                // A descendant wins even if the consumer also directly extends the ancestor.
                final boolean overridden =
                    scopes.keySet().stream()
                        .anyMatch(
                            other -> !other.equals(scope) && other.getHierarchy().contains(scope));
                if (!overridden) {
                  values.addAll(declaredValues);
                }
              });
          if (values.size() > 1) {
            throw new GradleException(
                "Conflicting javaagent options for "
                    + component.getDisplayName()
                    + " in configuration '"
                    + configuration.getName()
                    + "'. Configure one option string per agent.");
          }
          if (!values.isEmpty()) {
            options.put(component, values.iterator().next());
          }
        });
    return options;
  }
}
