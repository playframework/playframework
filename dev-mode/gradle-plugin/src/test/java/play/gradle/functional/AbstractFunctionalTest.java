/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.functional;

import static java.nio.charset.StandardCharsets.UTF_8;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import play.gradle.plugin.PlayPlugin;

abstract class AbstractFunctionalTest {

  protected static final String ROUTES_GEN_PATH = "generated/sources/play/routes/main/";

  @TempDir Path projectPath;

  Path projectSourcePath;

  GradleRunner runner;

  Configuration freemarkerConf;

  protected abstract Path getProjectSourcePath();

  protected abstract String getBuildFileContent();

  protected String getSettingsFileContent() {
    return "";
  }

  static String getScalaVersion() {
    return System.getProperty("scala.version", PlayPlugin.DEFAULT_SCALA_VERSION);
  }

  static String getPlayVersion() {
    return System.getProperty("play.version");
  }

  protected Path projectSourcePath(String path) {
    return projectSourcePath.resolve(path);
  }

  protected Path projectPath(String path) {
    return projectPath.resolve(path);
  }

  protected Path projectBuildPath(String path) {
    return projectPath.resolve("build/" + path);
  }

  protected void prepareProject() throws IOException {
    PathUtils.writeString(projectPath("build.gradle.kts"), getBuildFileContent(), UTF_8);
    PathUtils.writeString(projectPath("settings.gradle.kts"), getSettingsFileContent(), UTF_8);
  }

  @BeforeEach
  void init() throws IOException {
    projectSourcePath = getProjectSourcePath();
    runner =
        GradleRunner.create()
            .withProjectDir(projectPath.toFile())
            .withPluginClasspath()
            // Don't scare the errors in log https://github.com/gradle/gradle/issues/4201
            .withDebug(true)
            .forwardOutput();

    initFreemarker();
    prepareProject();
  }

  protected void initFreemarker() throws IOException {
    freemarkerConf = new Configuration(Configuration.VERSION_2_3_32);
    freemarkerConf.setDirectoryForTemplateLoading(projectSourcePath.toFile());
  }

  protected String templateProcess(String template, Map<String, Object> params) {
    StringWriter writer = new StringWriter();
    try {
      Template buildGradle = freemarkerConf.getTemplate(template);
      buildGradle.process(params, writer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  protected BuildResult build(String gradleVersion, String... args) {
    return runner.withGradleVersion(gradleVersion).withArguments(args).build();
  }

  static Stream<String> gradleVersions() {
    String latest = GradleVersion.current().getVersion();
    // https://docs.gradle.org/current/userguide/compatibility.html
    if (JavaVersion.current().compareTo(JavaVersion.VERSION_21) >= 0) { // Gradle 8.4+
      return Stream.of(latest);
    }
    if (JavaVersion.current().compareTo(JavaVersion.VERSION_17) >= 0) { // Gradle 7.3+
      return Stream.of("7.6.2", "8.0.2", latest);
    }
    // https://docs.gradle.org/current/userguide/scala_plugin.html#sec:configure_zinc_compiler
    if (getScalaVersion().equals("3")) { // Gradle 7.5+
      return Stream.of("7.6.2", "8.0.2", latest);
    }
    return Stream.of("7.1.1", "7.6.2", "8.0.2", latest);
  }
}
