/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core

import play.api.{ ApplicationLoader, BuiltInComponentsFromContext, Environment, Play }

package object test {

  /**
   * Run the given block of code with an application.
   */
  def withApplication[T](block: => T): T = {
    val app = new BuiltInComponentsFromContext(ApplicationLoader.createContext(Environment.simple())) {
      def router = play.api.routing.Router.empty
    }.application
    Play.start(app)
    try {
      block
    } finally {
      Play.stop(app)
    }
  }

}
