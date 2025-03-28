/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.tasks.Nested;

/**
 * The extension of the plugin allowing for configuring the target Scala version used for the
 * application.
 */
@Incubating
public abstract class PlayExtension {

  public static final String PLAY_EXTENSION_NAME = "play";

  private final Language lang;

  public PlayExtension(Language lang) {
    this.lang = lang;
  }

  @Nested
  public abstract RoutesSettings getRoutes();

  public void routes(Action<? super RoutesSettings> action) {
    action.execute(getRoutes());
  }

  @Nested
  public abstract AssetsSettings getAssets();

  public void assets(Action<? super AssetsSettings> action) {
    action.execute(getAssets());
  }

  public Language getLang() {
    return lang;
  }
}
