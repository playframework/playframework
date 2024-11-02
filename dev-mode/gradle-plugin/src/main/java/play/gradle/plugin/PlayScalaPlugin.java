/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import javax.inject.Inject;
import play.gradle.Language;

public class PlayScalaPlugin extends PlayPlugin {

  @Inject
  public PlayScalaPlugin() {
    super(Language.SCALA);
  }
}
