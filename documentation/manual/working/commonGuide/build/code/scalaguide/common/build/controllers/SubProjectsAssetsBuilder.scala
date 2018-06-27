/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package common.build.controllers {

  //#assets-builder
  import javax.inject._

  import play.api.http.HttpErrorHandler
  import scalaguide.common.build.controllers.AssetsBuilder

  class Assets @Inject() (
    errorHandler: HttpErrorHandler,
    assetsMetadata: controllers.AssetsMetadata
  ) extends AssetsBuilder(errorHandler, assetsMetadata)
  //#assets-builder

  package admin {
    //#admin-home-controller
    //###insert: package controllers.admin

    import play.api.mvc._
    import javax.inject.Inject

    class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

      def index = Action { implicit request =>
        Ok("admin")
      }
    }
    //#admin-home-controller
  }
}
