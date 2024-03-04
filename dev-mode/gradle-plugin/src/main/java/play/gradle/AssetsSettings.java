/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import org.gradle.api.Incubating;
import org.gradle.api.provider.Property;

@Incubating
public abstract class AssetsSettings {
  public abstract Property<String> getPath();
}
