/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.io.file.PathUtils;
import org.apache.groovy.util.Maps;
import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Check project `routes-compiler-routes-compilation`")
public class RoutesCompilerRoutesCompilationTest extends AbstractFunctionalTest {
  @Override
  protected Path getProjectSourcePath() {
    return Paths.get(
        "../sbt-plugin/src/sbt-test/play-sbt-plugin/routes-compiler-routes-compilation");
  }

  @Override
  protected String getBuildFileContent() {
    return templateProcess(
        "build.gradle.kts.ftlh",
        Maps.of(
            "scalaVersion", getScalaVersion(),
            "playVersion", getPlayVersion(),
            "javaVersion", JavaVersion.current()));
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

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test UP-TO-DATE behavior for build without changes in routes")
  void testUpToDateBuild(String gradleVersion) {
    var result = build(gradleVersion, "classes");

    var compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    var compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    result = build(gradleVersion, "classes");

    compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

    compileScalaResult = result.task(":compileScala");
    assertThat(compileScalaResult).isNotNull();
    assertThat(compileScalaResult.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test incremental compilation after add and delete routes")
  void testIncrementalBuild(String gradleVersion) throws IOException {
    Path routes = projectPath("conf/routes");
    Path generatedRoutes = projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala");

    // Delete main routes to compile only `module.routes`
    Files.delete(routes);

    var result = build(gradleVersion, "compilePlayRoutes");
    var compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(generatedRoutes).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/RoutesPrefix.scala")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/routes.java")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/ReverseRoutes.scala"))
        .doesNotExist();
    assertThat(
            projectBuildPath(
                ROUTES_GEN_PATH + "controllers/javascript/JavaScriptReverseRoutes.scala"))
        .doesNotExist();

    // Add routes
    Files.copy(projectSourcePath("conf/routes"), routes);

    result = build(gradleVersion, "compilePlayRoutes");
    compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(generatedRoutes).isNotEmptyFile();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/RoutesPrefix.scala")).isNotEmptyFile();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/routes.java")).isNotEmptyFile();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/ReverseRoutes.scala"))
        .isNotEmptyFile();
    assertThat(
            projectBuildPath(
                ROUTES_GEN_PATH + "controllers/javascript/JavaScriptReverseRoutes.scala"))
        .isNotEmptyFile();

    // Delete routes
    Files.delete(routes);

    result = build(gradleVersion, "compilePlayRoutes");
    compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(generatedRoutes).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/RoutesPrefix.scala")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/routes.java")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "controllers/ReverseRoutes.scala"))
        .doesNotExist();
    assertThat(
            projectBuildPath(
                ROUTES_GEN_PATH + "controllers/javascript/JavaScriptReverseRoutes.scala"))
        .doesNotExist();
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test build cache")
  void testBuildCache(String gradleVersion) throws IOException {
    Path routes = projectPath("conf/routes");

    var result = build(gradleVersion, "--build-cache", "compilePlayRoutes");
    var compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).isNotEmptyFile();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).isNotEmptyFile();

    build(gradleVersion, "clean");
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).doesNotExist();

    result = build(gradleVersion, "--build-cache", "compilePlayRoutes");
    compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.FROM_CACHE);
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).isNotEmptyFile();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).isNotEmptyFile();

    build(gradleVersion, "clean");
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).doesNotExist();

    Files.delete(routes);

    result = build(gradleVersion, "--build-cache", "compilePlayRoutes");

    compilePlayRoutes = result.task(":compilePlayRoutes");
    assertThat(compilePlayRoutes).isNotNull();
    assertThat(compilePlayRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "router/Routes.scala")).doesNotExist();
    assertThat(projectBuildPath(ROUTES_GEN_PATH + "module/Routes.scala")).isNotEmptyFile();
  }
}
