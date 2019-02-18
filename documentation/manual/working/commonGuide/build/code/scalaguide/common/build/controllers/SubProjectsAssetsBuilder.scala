/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package common.build.controllers {

  //#assets-builder
  import javax.inject._

  import play.api.http.HttpErrorHandler

  class Assets @Inject()(
      errorHandler: HttpErrorHandler,
      assetsMetadata: controllers.AssetsMetadata
  ) extends controllers.AssetsBuilder(errorHandler, assetsMetadata)
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
