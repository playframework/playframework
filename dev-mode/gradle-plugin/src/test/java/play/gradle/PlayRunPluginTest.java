/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.initialization.SettingsState;
import org.gradle.internal.resource.ResourceLocation;
import org.gradle.internal.resource.TextResource;
import org.gradle.invocation.DefaultGradle;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import play.gradle.task.PlayRun;

/** A simple unit test to check a Play Run Plugin */
class PlayRunPluginTest {

  private Project project;

  @BeforeEach
  void init() {
    project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("application");
    project.getPluginManager().apply("org.playframework.play");
    ((DefaultGradle) project.getGradle()).attachSettings(mockSettings());
  }

  /** Workaround <a href="https://github.com/gradle/gradle/issues/20301">gradle#20301</a> */
  private SettingsState mockSettings() {
    var locationMock = mock(ResourceLocation.class);
    when(locationMock.getFile()).thenReturn(new File("."));
    var resourcesMock = mock(TextResource.class);
    when(resourcesMock.getLocation()).thenReturn(locationMock);
    var settingsScriptMock = mock(ScriptSource.class);
    when(settingsScriptMock.getResource()).thenReturn(resourcesMock);
    var settingsInternal = mock(SettingsInternal.class);
    when(settingsInternal.getSettingsScript()).thenReturn(settingsScriptMock);
    SettingsState settings = mock(SettingsState.class);
    when(settings.getSettings()).thenReturn(settingsInternal);
    return settings;
  }

  @Test
  @DisplayName("Check classpath with submodules")
  void checkClasspathWithSubmodules() {
    Project javaLib = ProjectBuilder.builder().withParent(project).withName("java-lib").build();
    javaLib.getPluginManager().apply("java");

    Project scalaLib = ProjectBuilder.builder().withParent(project).withName("scala-lib").build();
    scalaLib.getPluginManager().apply("scala");

    project.getRepositories().add(project.getRepositories().mavenCentral());
    project.getDependencies().add("implementation", javaLib);
    project.getDependencies().add("implementation", scalaLib);

    ((DefaultProject) project).evaluate();

    assertThat(((PlayRun) project.getTasks().findByName("playRun")).getClasses())
        .contains(
            project.getLayout().getBuildDirectory().file("classes/scala/main").get().getAsFile(),
            project.getLayout().getBuildDirectory().file("classes/java/main").get().getAsFile(),
            project.getLayout().getBuildDirectory().file("resources/main").get().getAsFile(),
            javaLib.getLayout().getBuildDirectory().file("classes/java/main").get().getAsFile(),
            javaLib.getLayout().getBuildDirectory().file("resources/main").get().getAsFile(),
            scalaLib.getLayout().getBuildDirectory().file("classes/scala/main").get().getAsFile(),
            scalaLib.getLayout().getBuildDirectory().file("classes/java/main").get().getAsFile(),
            scalaLib.getLayout().getBuildDirectory().file("resources/main").get().getAsFile());
  }
}
