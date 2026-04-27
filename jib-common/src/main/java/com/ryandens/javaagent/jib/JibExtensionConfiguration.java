package com.ryandens.javaagent.jib;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

/**
 * Extra configuration for {@link JavaagentJibExtension}. Declares the javaagent files that should
 * be copied into the container image and referenced via {@code -javaagent} JVM flags.
 *
 * <p>Instances are created by Jib's plugin extension mechanism via {@link ObjectFactory}, so this
 * class must have a single-argument constructor accepting {@link ObjectFactory}.
 */
public class JibExtensionConfiguration {

  private final ListProperty<File> javaagentFiles;

  /**
   * Instantiated by Jib's plugin extension mechanism
   *
   * <p>Not compatible with configuration cache due to usage of Project at task execution time.
   */
  @Inject
  public JibExtensionConfiguration(final Project project) {
    this(project.getObjects());
  }

  public JibExtensionConfiguration(final ObjectFactory objectFactory) {
    javaagentFiles = objectFactory.listProperty(File.class);
  }

  /**
   * Returns the list of javaagent {@link File}s to include in the container image. Each file is
   * placed under {@code /opt/jib-agents/} in the image and added to the container entrypoint as a
   * {@code -javaagent} flag. Package-private: intended to be accessed only by {@link
   * JavaagentJibExtension}.
   */
  @Input
  ListProperty<File> getJavaagentFiles() {
    return javaagentFiles;
  }
}
