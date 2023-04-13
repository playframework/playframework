/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.dependencyinjection.controllers;

import controllers.AssetsMetadata;
import play.api.Environment;
import play.api.http.HttpErrorHandler;

public class Assets extends controllers.Assets {
  public Assets(HttpErrorHandler errorHandler, AssetsMetadata meta, Environment env) {
    super(errorHandler, meta, env);
  }
}
