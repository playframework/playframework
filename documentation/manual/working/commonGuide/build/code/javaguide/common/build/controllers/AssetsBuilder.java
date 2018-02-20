/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

//###replace: package controllers.admin;
package javaguide.common.build.controllers;
// #assets-builder
import play.api.mvc.*;
import controllers.AssetsMetadata;
import play.api.http.HttpErrorHandler;

import javax.inject.Inject;

class Assets extends controllers.Assets {

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
