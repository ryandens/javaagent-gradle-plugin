package com.ryandens.javaagent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
   */
  public static void configureJavaForkOptions(
      JavaForkOptions javaForkOptions, Provider<Set<File>> javaagentConfiguration) {
    final List<CommandLineArgumentProvider> list = new ArrayList<>();

    //noinspection Convert2Lambda
    list.add(
        new CommandLineArgumentProvider() {
          @Override
          public Iterable<String> asArguments() {
            return javaagentConfiguration.get().stream()
                // Use the absolute path rather than the canonical path. On Windows CI the Gradle
                // caches live behind a symlink/junction, and getCanonicalPath() resolves it to a
                // real path whose round-trip through the OpenTelemetry agent's own jar-location
                // logic yields a name that appendToBootstrapClassLoaderSearch rejects with
                // IllegalArgumentException. The distribution start scripts already use a plain,
                // unresolved path, which works.
                .map(
                    file -> {
                      String arg = "-javaagent:" + file.getAbsolutePath();
                      // TEMP diagnostic: print the exact -javaagent arg passed to the forked JVM
                      System.err.println("DIAG_JAVAAGENT_ARG=[" + arg + "]");
                      return arg;
                    })
                .collect(Collectors.toList());
          }
        });
    javaForkOptions.getJvmArgumentProviders().addAll(list);
  }
}
