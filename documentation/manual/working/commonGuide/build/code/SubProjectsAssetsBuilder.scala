/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package common.build

package object assets {
  type Assets = play.controllers.assets.Assets
  val Assets = play.controllers.assets.Assets
}

package controllers.admin {

//#assets-builder
import play.controllers.assets._

import play.api.http.HttpErrorHandler
import javax.inject._

class Assets @Inject() (errorHandler: HttpErrorHandler, assetsMetadata: AssetsMetadata) extends AssetsBuilder(errorHandler, assetsMetadata)
//#assets-builder

import play.api.mvc._

class HomeController @Inject()(components: ControllerComponents) extends AbstractController(components) {
  def index = Action(Ok)
}

}
