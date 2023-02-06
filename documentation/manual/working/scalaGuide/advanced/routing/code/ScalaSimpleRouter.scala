/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import play.api.http.HttpConfiguration
import play.api.inject._
import play.api.inject.guice.GuiceApplicationLoader
import play.api.inject.guice.GuiceableModule
import play.api.mvc._
import play.api.routing.sird._
import play.api.routing.Router
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.ApplicationLoader

//#load-guice
class ScalaSimpleRouter @Inject() (val Action: DefaultActionBuilder) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/") =>
      Action {
        Results.Ok
      }
  }
}

@Singleton
class ScalaRoutesProvider @Inject() (playSimpleRouter: ScalaSimpleRouter, httpConfig: HttpConfiguration)
    extends Provider[Router] {
  lazy val get = playSimpleRouter.withPrefix(httpConfig.context)
}

class ScalaGuiceAppLoader extends GuiceApplicationLoader {
  protected override def overrides(context: ApplicationLoader.Context): Seq[GuiceableModule] = {
    super.overrides(context) :+ (bind[Router].toProvider[ScalaRoutesProvider]: GuiceableModule)
  }
}
//#load-guice
