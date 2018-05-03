/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import play.api._

package object test {

  /**
   * Run the given block of code with an application.
   */
  def withApplication[T](block: => T): T = {
    val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple())) with NoHttpFiltersComponents {
      def router = play.api.routing.Router.empty
    }.application
    Play.start(app)
    try {
      block
    } finally {
      Play.stop(app)
    }
  }

  def withApplication[T](block: Application => T): T = {
    val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple())) with NoHttpFiltersComponents {
      def router = play.api.routing.Router.empty
    }.application
    Play.start(app)
    try {
      block(app)
    } finally {
      Play.stop(app)
    }
  }

}
