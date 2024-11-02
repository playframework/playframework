/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import play.gradle.internal.Utils;

/** A simple unit test to check a Play Gradle Plugin */
class PlayPluginTest {

  private Project project;

  @BeforeEach
  void init() {
    project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("org.playframework.play-java");
  }

  @Test
  @DisplayName("Play extension should be registered")
  void extensionShouldBeRegistered() {
    PlayExtension ext = Utils.playExtension(project);
    assertThat(ext).isNotNull();
    assertThat(ext.getLang()).isEqualTo(Language.JAVA);
  }
}
