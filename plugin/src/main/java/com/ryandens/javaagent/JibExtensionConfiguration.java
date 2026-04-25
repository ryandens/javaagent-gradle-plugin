package com.ryandens.javaagent;

import java.io.File;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

public class JibExtensionConfiguration {

  private final ListProperty<File> javaagentFiles;

  /** Used by tiny-jib */
  public JibExtensionConfiguration(final ObjectFactory objectFactory) {
    javaagentFiles = objectFactory.listProperty(File.class);
  }

  @Input
  ListProperty<File> getJavaagentFiles() {
    return javaagentFiles;
  }
}
