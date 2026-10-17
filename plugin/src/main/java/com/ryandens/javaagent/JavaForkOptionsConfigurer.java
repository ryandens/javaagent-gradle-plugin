package com.ryandens.javaagent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.provider.Provider;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.process.JavaForkOptions;

/**
 * Utility class for configuring {@link JavaForkOptions}
 *
 * <p>This class is written in Java as a result of <a
 * href="https://docs.gradle.org/7.5.1/userguide/validation_problems.html#implementation_unknown">this
 * limitation with gradle task inputs</a>
 */
public final class JavaForkOptionsConfigurer {

  /** Prevent instantiation for utility class. */
  private JavaForkOptionsConfigurer() {}

  /**
   * Configures the provided {@link JavaForkOptions} to use the provided javaagents when launching
   *
   * @param javaForkOptions to be configured
   * @param javaagentConfiguration files to be added as javaagents
   * @param optionsByFilePath options to append after {@code =}, keyed by agent file path (see
   *     {@link AgentOptionsResolver})
   */
  public static void configureJavaForkOptions(
      JavaForkOptions javaForkOptions,
      Provider<Set<File>> javaagentConfiguration,
      Provider<Map<String, String>> optionsByFilePath) {
    final List<CommandLineArgumentProvider> list = new ArrayList<>();

    //noinspection Convert2Lambda
    list.add(
        new CommandLineArgumentProvider() {
          @Override
          public Iterable<String> asArguments() {
            final Map<String, String> options = optionsByFilePath.get();
            return javaagentConfiguration.get().stream()
                .map(
                    file -> {
                      try {
                        final String options0 = options.get(file.getCanonicalPath());
                        final String suffix = options0 == null ? "" : "=" + options0;
                        return "-javaagent:" + file.getCanonicalPath() + suffix;
                      } catch (IOException e) {
                        throw new UncheckedIOException(e);
                      }
                    })
                .collect(Collectors.toList());
          }
        });
    javaForkOptions.getJvmArgumentProviders().addAll(list);
  }
}
