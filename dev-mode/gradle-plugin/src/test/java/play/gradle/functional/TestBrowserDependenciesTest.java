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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;

/**
 * The same applications as the sbt scripted test, but resolved from Play's Maven POMs, which have
 * to declare the test browser's dependencies as optional.
 */
@DisplayName("Check project `test-browser-dependencies`")
public class TestBrowserDependenciesTest extends AbstractFunctionalTest {
  @Override
  protected Path getProjectSourcePath() {
    return Paths.get("../sbt-plugin/src/sbt-test/play-sbt-plugin/test-browser-dependencies");
  }

  @Override
  protected String getBuildFileContent() {
    return "";
  }

  @Override
  protected String getSettingsFileContent() {
    return templateProcess("settings.gradle.kts.ftlh", Map.of());
  }

  @Override
  protected void prepareProject() throws IOException {
    super.prepareProject();
    prepareSubproject("without-browser", false, "test");
    prepareSubproject("with-browser", true, "app", "conf", "test");
  }

  private void prepareSubproject(String subproject, boolean withBrowser, String... dirs)
      throws IOException {
    Files.createDirectories(projectPath(subproject));
    for (String dir : dirs) {
      PathUtils.copyDirectory(
          projectSourcePath(subproject + "/" + dir), projectPath(subproject + "/" + dir));
    }
    String buildFileContent =
        templateProcess(
            "build.gradle.kts.ftlh",
            Map.of(
                "scalaVersion", getScalaVersion(),
                "playVersion", getPlayVersion(),
                "javaVersion", JavaVersion.current(),
                "withBrowser", withBrowser));
    PathUtils.writeString(projectPath(subproject + "/build.gradle.kts"), buildFileContent, UTF_8);
  }

  @ParameterizedTest
  @MethodSource("gradleVersions")
  @DisplayName("Test that only play-test-browser brings the test browser's dependencies")
  void testBrowserDependencies(String gradleVersion) throws Exception {
    BuildResult result = build(gradleVersion, "check");

    for (String subproject : List.of("without-browser", "with-browser")) {
      BuildTask testResult = result.task(":" + subproject + ":test");
      assertThat(testResult).isNotNull();
      assertThat(testResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
    assertTestsExecuted("without-browser", "WithoutBrowserSpec");
    assertTestsExecuted("without-browser", "WithoutBrowserTest");
    assertTestsExecuted("with-browser", "WithBrowserSpec");
    assertTestsExecuted("with-browser", "WithBrowserTest");
  }

  private void assertTestsExecuted(String subproject, String testClass) throws Exception {
    Path report = projectPath(subproject + "/build/test-results/test/TEST-" + testClass + ".xml");
    assertThat(report).isNotEmptyFile();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    Element suite = factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
    assertThat(suite.getTagName()).as("suite in %s", report).isEqualTo("testsuite");
    assertThat(Integer.parseInt(suite.getAttribute("tests")))
        .as("executed tests in %s", report)
        .isPositive();
    for (String attribute : List.of("skipped", "failures", "errors")) {
      assertThat(Integer.parseInt(suite.getAttribute(attribute)))
          .as("%s in %s", attribute, report)
          .isZero();
    }
  }
}
