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

public final class JavaForkOptionsConfigurer {

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
