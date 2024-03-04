/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static play.gradle.internal.Utils.javaApplicationExtension;

import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@Incubating
public class PlayApplicationPlugin implements Plugin<Project> {

  @Override
  public void apply(@NotNull Project project) {
    javaApplicationExtension(project).getMainClass().set("play.core.server.ProdServerStart");
  }
}
