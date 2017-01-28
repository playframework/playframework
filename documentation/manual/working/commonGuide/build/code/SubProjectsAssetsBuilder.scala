/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package common.build

//#assets-builder
package controllers.admin

import _root_.controllers.AssetsMetadata

import play.api.http.HttpErrorHandler
import javax.inject._

class Assets @Inject() (errorHandler: HttpErrorHandler, assetsMetadata: AssetsMetadata) extends controllers.AssetsBuilder(errorHandler, assetsMetadata)
//#assets-builder

import play.api.mvc._

class HomeController extends Controller {
  def index = Action(Ok)
}
