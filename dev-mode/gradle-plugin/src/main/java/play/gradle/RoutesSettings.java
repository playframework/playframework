/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle;

import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

@Incubating
public abstract class RoutesSettings {

  public abstract Property<Boolean> getNamespaceReverseRouter();

  public abstract Property<Boolean> getGenerateReverseRouter();

  public abstract Property<Boolean> getGenerateJsReverseRouter();

  public abstract SetProperty<String> getImports();

  public abstract ListProperty<Project> getAggregateReverseRoutes();
}
