/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.dependencyinjection

package controllers {
import play.api._
import play.api.mvc._

class SomeController extends Controller {
    def index() = Action {
      Ok("hello world!")
    }
  }
}
