/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
import play.api.ApplicationLoader.Context
import play.api._
import play.api.routing.Router
import router.RoutingDslBuilder

//#load
class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  lazy val router = Router.from {
       RoutingDslBuilder.getRouter.asScala.routes
  }
}
//#load
