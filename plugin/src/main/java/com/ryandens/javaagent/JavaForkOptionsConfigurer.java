package com.ryandens.javaagent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.Configuration;
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

  /**
   * Configures the provided {@link JavaForkOptions} to use the provided javaagents when laucnhing
   *
   * @param javaForkOptions to be configured
   * @param javaagentConfiguration files to be added as javaagents
   */
  public static void configureJavaForkOptions(
      JavaForkOptions javaForkOptions,
      NamedDomainObjectProvider<Configuration> javaagentConfiguration) {
    final List<CommandLineArgumentProvider> list = new ArrayList<>();

    //noinspection Convert2Lambda
    list.add(
        new CommandLineArgumentProvider() {
          @Override
          public Iterable<String> asArguments() {
            return javaagentConfiguration.get().getFiles().stream()
                .map(
                    file -> {
                      try {
                        return "-javaagent:" + file.getCanonicalPath();
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
