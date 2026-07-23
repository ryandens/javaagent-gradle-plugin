package com.ryandens.javaagent.jib;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
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

  private final MapProperty<String, String> agentOptions;

  /**
   * Instantiated by Jib's plugin extension mechanism
   *
   * <p>Not compatible with configuration cache due to usage of Project at task execution time.
   *
   * @param project this extension is being applied to. Not ideal for configuration cache, but
   *     mandated by jib extension architecture.
   */
  @Inject
  public JibExtensionConfiguration(final Project project) {
    this(project.getObjects());
  }

  /**
   * Constructor to allows for instantiation of this extension configuration by alternate jib-like
   * frameworks such as <a href="https://github.com/pschichtel/tiny-jib">tel.schich.tinyjib</a>
   *
   * @param objectFactory to create {@link org.gradle.api.provider.Property} objects required by
   *     this class.
   */
  public JibExtensionConfiguration(final ObjectFactory objectFactory) {
    javaagentFiles = objectFactory.listProperty(File.class);
    agentOptions = objectFactory.mapProperty(String.class, String.class);
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

  /**
   * Returns the agent options keyed by agent file name. When an agent's file name has an entry
   * here, the corresponding value is appended to its {@code -javaagent} flag as {@code =<options>}.
   * Package-private: intended to be accessed only by {@link JavaagentJibExtension}.
   */
  @Input
  MapProperty<String, String> getAgentOptions() {
    return agentOptions;
  }
}
