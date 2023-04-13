/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// #assets-builder
// ###replace: package controllers.admin;
package javaguide.common.build.controllers;

import controllers.AssetsMetadata;
import javax.inject.Inject;
import play.api.Environment;
import play.api.http.HttpErrorHandler;
import play.api.mvc.*;

public class Assets extends controllers.Assets {

  @Inject
  public Assets(HttpErrorHandler errorHandler, AssetsMetadata meta, Environment env) {
    super(errorHandler, meta, env);
  }

  public Action<AnyContent> at(String path, String file) {
    boolean aggressiveCaching = true;
    return super.at(path, file, aggressiveCaching);
  }
}
// #assets-builder
