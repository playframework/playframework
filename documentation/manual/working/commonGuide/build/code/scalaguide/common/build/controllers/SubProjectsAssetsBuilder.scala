/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package common.build.controllers {
  // #assets-builder
  import javax.inject._

  import play.api.http.HttpErrorHandler
  import play.api.Environment

  class Assets @Inject() (
      errorHandler: HttpErrorHandler,
      assetsMetadata: controllers.AssetsMetadata,
      environment: Environment
  ) extends controllers.AssetsBuilder(errorHandler, assetsMetadata, environment)
  // #assets-builder

  package admin {
    // #admin-home-controller
    // ###insert: package controllers.admin

    import javax.inject.Inject

    import play.api.mvc._

    class HomeController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
      def index: Action[AnyContent] = Action { implicit request => Ok("admin") }
    }
    // #admin-home-controller
  }
}
