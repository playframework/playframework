/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package common.build

//#assets-builder
package controllers.admin

import play.api.http.HttpErrorHandler
import javax.inject._

class Assets @Inject() (errorHandler: HttpErrorHandler) extends controllers.AssetsBuilder(errorHandler)
//#assets-builder

import play.api.mvc._

class HomeController extends Controller {
  def index = Action(Ok)
}
