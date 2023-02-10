/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.sird._
import play.api.routing.Router
import play.api.ApplicationLoader.Context

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
