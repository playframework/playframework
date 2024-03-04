/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.internal;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.work.ChangeType;
import org.gradle.workers.WorkParameters;

/** Parameters of Routes compilation work action. */
public interface RoutesCompileParams extends WorkParameters {

  Property<ChangeType> getChangeType();

  RegularFileProperty getRoutesFile();

  DirectoryProperty getDestinationDirectory();

  Property<Boolean> getNamespaceReverseRouter();

  Property<Boolean> getGenerateReverseRouter();

  Property<Boolean> getGenerateJsReverseRouter();

  Property<Boolean> getGenerateForwardsRouter();

  SetProperty<String> getImports();
}
