/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.controllers;

import controllers.*;
import play.Environment;
import play.components.ConfigurationComponents;
import play.components.FileMimeTypesComponents;
import play.components.HttpErrorHandlerComponents;
import play.inject.ApplicationLifecycle;

/** Java components for Assets. */
public interface AssetsComponents
    extends ConfigurationComponents, HttpErrorHandlerComponents, FileMimeTypesComponents {

  Environment environment();

  ApplicationLifecycle applicationLifecycle();

  default AssetsConfiguration assetsConfiguration() {
    return AssetsConfiguration$.MODULE$.fromConfiguration(
        configuration(), environment().asScala().mode());
  }

  default AssetsMetadata assetsMetadata() {
    return new AssetsMetadataProvider(
            environment().asScala(),
            assetsConfiguration(),
            fileMimeTypes().asScala(),
            applicationLifecycle().asScala())
        .get();
  }

  default AssetsFinder assetsFinder() {
    return assetsMetadata().finder();
  }

  default Assets assets() {
    return new Assets(scalaHttpErrorHandler(), assetsMetadata(), environment().asScala());
  }
}
