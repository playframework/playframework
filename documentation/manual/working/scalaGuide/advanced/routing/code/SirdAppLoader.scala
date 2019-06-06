/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._

//#load
class SirdAppLoader extends ApplicationLoader {
  def load(context: Context) = {
    new SirdComponents(context).application
  }
}

class SirdComponents(context: Context) extends BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
  lazy val router = Router.from {
    case GET(p"/hello/$to") =>
      Action {
        Ok(s"Hello $to")
      }
  }
}
//#load
