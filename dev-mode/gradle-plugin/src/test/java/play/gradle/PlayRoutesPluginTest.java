/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static play.gradle.internal.Utils.javaPluginExtension;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import play.gradle.plugin.PlayPlugin;

/** A simple unit test to check a Play Routes Plugin */
class PlayRoutesPluginTest {

  private Project project;

  @BeforeEach
  void init() {
    project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("org.playframework.play-java");
  }

  @Test
  @DisplayName("Play Routes configuration should be registered")
  void routesConfigurationShouldBeRegistered() {
    Configuration conf = project.getConfigurations().findByName("playRoutes");
    assertThat(conf).isNotNull();
    assertThat(conf.isTransitive()).isTrue();
    assertThat(conf.isVisible()).isFalse();
    ((DefaultConfiguration) conf).runDependencyActions();
    assertThat(conf.getDependencies())
        .anyMatch(
            dependency ->
                PlayPlugin.PLAY_GROUP_ID.equals(dependency.getGroup())
                    && dependency.getName().startsWith("play-routes-compiler"));
  }

  @Test
  @DisplayName("Routes source directory set should be registered for main source set")
  void routesSourceDirectorySetShouldBeRegisteredForMainSourceSet() {
    checkRoutesSourceDirectorySet(javaPluginExtension(project).getSourceSets().getByName("main"));
  }

  @Test
  @DisplayName("Routes source directory set should be registered for test source set")
  void routesSourceDirectorySetShouldBeRegisteredForTestSourceSet() {
    checkRoutesSourceDirectorySet(javaPluginExtension(project).getSourceSets().getByName("test"));
  }

  @Test
  @DisplayName("Routes compile task should be registered for main/test source sets")
  void compileTasksShouldBeRegistered() {
    assertThat(project.getTasks().findByName("compilePlayRoutes")).isNotNull();
    assertThat(project.getTasks().findByName("compileTestPlayRoutes")).isNotNull();
  }

  private void checkRoutesSourceDirectorySet(SourceSet sourceSet) {
    assertThat(sourceSet).isNotNull();
    SourceDirectorySet routesSourceSet =
        (SourceDirectorySet) sourceSet.getExtensions().findByName("routes");
    assertThat(routesSourceSet).isNotNull();
  }
}
