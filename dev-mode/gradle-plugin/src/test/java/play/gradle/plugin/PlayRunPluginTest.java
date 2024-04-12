/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PlayRunPluginTest {
  @TempDir() Path testProjectDir;

  @BeforeEach
  void setup() throws IOException {
    var playProjectDir = testProjectDir.resolve("play-project");
    Files.createDirectories(playProjectDir);
    var playBuildFile = playProjectDir.resolve("build.gradle");
    Files.writeString(
        playBuildFile,
        """
            plugins {
                id 'scala'
                id 'org.playframework.play'
            }
            dependencies {
              implementation project(':scala-project')
              implementation project(':java-project')
            }
            project.task('getPlayRunClasses') {
              doLast {
                println(project.tasks.named('playRun').get().getClasses().getAsPath())
              }
            }
        """);
    Files.createDirectories(playProjectDir.resolve("src/main/scala"));

    var scalaProjectDir = testProjectDir.resolve("scala-project");
    Files.createDirectories(scalaProjectDir);
    var scalaBuildFile = scalaProjectDir.resolve("build.gradle");
    Files.writeString(
        scalaBuildFile,
        """
            plugins {
                id 'scala'
            }
        """);
    Files.createDirectories(scalaProjectDir.resolve("src/main/java"));
    Files.createDirectories(scalaProjectDir.resolve("src/main/scala"));

    var javaOnlyProjectDir = testProjectDir.resolve("java-project");
    Files.createDirectories(javaOnlyProjectDir);
    var javaBuildFile = javaOnlyProjectDir.resolve("build.gradle");
    Files.writeString(
        javaBuildFile,
        """
            plugins {
                id 'java'
            }
        """);
    Files.createDirectories(javaOnlyProjectDir.resolve("src/main/java"));

    var buildFile = testProjectDir.resolve("build.gradle");
    Files.writeString(
        buildFile,
        """
            plugins {
                id 'scala'
                id 'org.playframework.play' apply false
            }
            repositories {
                mavenCentral()
            }
        """);
    var settingsFiles = testProjectDir.resolve("settings.gradle");
    Files.writeString(
        settingsFiles,
        """
            rootProject.name = 'play-test'
            include 'play-project'
            include 'scala-project'
            include 'java-project'
        """);
  }

  @Test
  void playRunClassPathContainsJavaScalaAndResourcesOutputDirs() {
    var result = runTasks("getPlayRunClasses", "--stacktrace");
    var output = result.getOutput();
    System.out.println(output);
    assertThat(output).contains("BUILD SUCCESSFUL");
    assertThat(output).contains("play-project/build/classes/scala/main");
    assertThat(output).contains("play-project/build/classes/java/main");
    assertThat(output).contains("play-project/build/resources/main");
    assertThat(output).contains("scala-project/build/classes/scala/main");
    assertThat(output).contains("scala-project/build/classes/java/main");
    assertThat(output).contains("scala-project/build/resources/main");
    assertThat(output).contains("java-project/build/resources/main");
    assertThat(output).contains("java-project/build/classes/java/main");
    assertThat(output).doesNotContain("play-project/build/classes/java/test");
    assertThat(output).doesNotContain("play-project/build/classes/scala/test");
    assertThat(output).doesNotContain("play-project/build/resources/test");
    assertThat(output).doesNotContain("scala-project/build/classes/scala/test");
    assertThat(output).doesNotContain("scala-project/build/classes/java/test");
    assertThat(output).doesNotContain("scala-project/build/resources/test");
    assertThat(output).doesNotContain("java-project/build/classes/java/test");
    assertThat(output).doesNotContain("java-project/build/resources/test");
  }

  private BuildResult runTasks(String... args) {
    return GradleRunner.create()
        .withGradleVersion("8.4")
        .withProjectDir(testProjectDir.toFile())
        .withArguments(args)
        .withPluginClasspath()
        .build();
  }
}
