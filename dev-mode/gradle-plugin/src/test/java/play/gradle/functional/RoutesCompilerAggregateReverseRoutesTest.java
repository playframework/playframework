/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.functional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.file.PathUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Check project `routes-compiler-aggregate-reverse-routes`")
public class RoutesCompilerAggregateReverseRoutesTest extends AbstractFunctionalTest {

  private static final List<String> PLAY_PROJECTS = List.of("common", "a", "b", "c");

  @Override
  protected Path getProjectSourcePath() {
    return Paths.get(
        "../sbt-plugin/src/sbt-test/play-sbt-plugin/routes-compiler-aggregate-reverse-routes");
  }

  @Override
  protected String getBuildFileContent() {
    return templateProcess("build.gradle.kts.ftlh", Map.of());
  }

  @Override
  protected String getSettingsFileContent() {
    return templateProcess("settings.gradle.kts.ftlh", Map.of());
  }

  @Override
  protected void prepareProject() throws IOException {
    super.prepareProject();
    PathUtils.copyDirectory(projectSourcePath("conf"), projectPath("conf"));
    for (String project : List.of("common", "a", "b", "c", "nonplay")) {
      Files.createDirectories(projectPath(project));
      PathUtils.writeString(
          projectPath(project + "/build.gradle.kts"),
          templateProcess(project + "/build.gradle.kts.ftlh", Map.of()),
          UTF_8);
    }
    for (String project : List.of("a", "b", "c")) {
      PathUtils.copyDirectory(projectSourcePath(project + "/conf"), projectPath(project + "/conf"));
    }
  }

  private Path generated(String project, String path) {
    String prefix = project.isEmpty() ? "" : project + "/";
    return projectPath(prefix + "build/" + ROUTES_GEN_PATH + path);
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Reverse routes are aggregated into a single project")
  void testAggregateReverseRoutes(String gradleVersion) {
    BuildResult result = build(gradleVersion, "compilePlayRoutes");

    for (String project : PLAY_PROJECTS) {
      var compileRoutes = result.task(":" + project + ":compilePlayRoutes");
      assertThat(compileRoutes).isNotNull();
      assertThat(compileRoutes.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    // The aggregating project generates the reverse routers of the projects it aggregates
    for (String project : List.of("a", "b", "c")) {
      assertThat(generated("common", "controllers/" + project + "/ReverseRoutes.scala"))
          .isNotEmptyFile();
      assertThat(generated("common", "controllers/" + project + "/routes.java")).isNotEmptyFile();
      assertThat(
              generated(
                  "common", "controllers/" + project + "/javascript/JavaScriptReverseRoutes.scala"))
          .isNotEmptyFile();
    }

    // The aggregated projects generate their forwards router only
    for (String project : List.of("a", "b", "c")) {
      assertThat(generated(project, project + "/Routes.scala")).isNotEmptyFile();
      assertThat(generated(project, "controllers/" + project + "/ReverseRoutes.scala"))
          .doesNotExist();
      assertThat(generated(project, "controllers/" + project + "/routes.java")).doesNotExist();
    }

    assertThat(generated("", "router/Routes.scala")).isNotEmptyFile();
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Aggregation after add and delete routes of an aggregated project")
  void testIncrementalAggregation(String gradleVersion) throws IOException {
    Path routes = projectPath("a/conf/a.routes");
    Path aggregated = generated("common", "controllers/a/ReverseRoutes.scala");

    build(gradleVersion, "compilePlayRoutes");
    assertThat(aggregated).isNotEmptyFile();

    Files.delete(routes);

    build(gradleVersion, "compilePlayRoutes");
    assertThat(aggregated).doesNotExist();
    assertThat(generated("common", "controllers/b/ReverseRoutes.scala")).isNotEmptyFile();

    Files.copy(projectSourcePath("a/conf/a.routes"), routes);

    build(gradleVersion, "compilePlayRoutes");
    assertThat(aggregated).isNotEmptyFile();
  }
}
