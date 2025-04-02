/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.io.file.PathUtils;
import org.apache.groovy.util.Maps;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Check project `routes-compiler-routes-compilation-java`")
public class RoutesCompilerRoutesCompilationJavaTest extends AbstractFunctionalTest {
  @Override
  protected Path getProjectSourcePath() {
    return Paths.get(
        "../sbt-plugin/src/sbt-test/play-sbt-plugin/routes-compiler-routes-compilation-java");
  }

  @Override
  protected String getBuildFileContent() {
    return templateProcess(
        "build.gradle.kts.ftlh",
        Maps.of(
            "scalaVersion", getScalaVersion(),
            "playVersion", getPlayVersion()));
  }

  @Override
  protected String getSettingsFileContent() {
    return templateProcess("settings.gradle.kts.ftlh", Map.of());
  }

  @Override
  protected void prepareProject() throws IOException {
    super.prepareProject();
    PathUtils.copyDirectory(projectSourcePath("app"), projectPath("app"));
    PathUtils.copyDirectory(projectSourcePath("conf"), projectPath("conf"));
    PathUtils.copyDirectory(projectSourcePath("public"), projectPath("public"));
    PathUtils.copyDirectory(projectSourcePath("tests"), projectPath("tests"));
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test common check")
  void testCommonBuild(String gradleVersion) {
    BuildResult result = build(gradleVersion, "check");

    BuildTask compileRoutesResult = result.task(":compilePlayRoutes");
    assertThat(compileRoutesResult).isNotNull();
    assertThat(compileRoutesResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(
            projectBuildPath(
                ROUTES_GEN_PATH + "controllers/javascript/JavaScriptReverseRoutes.scala"))
        .exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/module/ReverseRoutes.scala"))
        .exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/module/routes.java")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/ReverseRoutes.scala")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/routes.java")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/RoutesPrefix.scala")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).exists();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/RoutesPrefix.scala")).exists();

    BuildTask compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath("classes/scala/main/router/Routes.class")).isNotEmptyFile();

    BuildTask checkResult = result.task(":check");
    assertThat(checkResult).isNotNull();
    assertThat(checkResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }
}
