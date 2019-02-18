/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.dependencyinjection.controllers;

import controllers.AssetsMetadata;
import play.api.http.HttpErrorHandler;

public class Assets extends controllers.Assets {
  public Assets(HttpErrorHandler errorHandler, AssetsMetadata meta) {
    super(errorHandler, meta);
  }
}
