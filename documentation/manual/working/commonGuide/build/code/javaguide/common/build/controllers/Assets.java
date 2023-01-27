/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// #assets-builder
// ###replace: package controllers.admin;
package javaguide.common.build.controllers;

import controllers.AssetsMetadata;
import javax.inject.Inject;
import play.api.http.HttpErrorHandler;
import play.api.mvc.*;

public class Assets extends controllers.Assets {

  @Inject
  public Assets(HttpErrorHandler errorHandler, AssetsMetadata meta) {
    super(errorHandler, meta);
  }

  public Action<AnyContent> at(String path, String file) {
    boolean aggressiveCaching = true;
    return super.at(path, file, aggressiveCaching);
  }
}
// #assets-builder
