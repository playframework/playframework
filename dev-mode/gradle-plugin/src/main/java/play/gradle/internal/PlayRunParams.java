/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PlayRunParams {
  private final List<File> dependenciesClasspath;
  private final List<File> applicationClasspath;
  private final List<File> assetsDirectories;
  private final String assetsPath;
  private final File projectPath;
  private final int httpPort;

  private final Map<String, String> devSettings;

  public PlayRunParams(
      List<File> dependenciesClasspath,
      List<File> applicationClasspath,
      List<File> assetsDirectories,
      String assetsPath,
      File projectPath,
      int httpPort,
      Map<String, String> devSettings) {
    this.dependenciesClasspath = dependenciesClasspath;
    this.applicationClasspath = applicationClasspath;
    this.assetsDirectories = assetsDirectories;
    this.assetsPath = assetsPath;
    this.projectPath = projectPath;
    this.httpPort = httpPort;
    this.devSettings = devSettings;
  }

  public List<File> getDependencyClasspath() {
    return dependenciesClasspath;
  }

  public List<File> getApplicationClasspath() {
    return applicationClasspath;
  }

  public List<File> getAssetsDirectories() {
    return assetsDirectories;
  }

  public File getProjectPath() {
    return projectPath;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public String getAssetsPrefix() {
    return assetsPath;
  }

  public Map<String, String> getDevSettings() {
    return devSettings;
  }
}
