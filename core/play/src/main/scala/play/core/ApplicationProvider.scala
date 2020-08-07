/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.io._
import scala.util.Try
import scala.util.Success

import play.api._
import play.api.mvc._

/**
 * Provides information about a Play Application running inside a Play server.
 */
trait ApplicationProvider {

  /**
   * Get the application. In dev mode this lazily loads the application.
   *
   * NOTE: This should be called once per request. Calling multiple times may result in multiple compilations.
   */
  def get: Try[Application]
}

object ApplicationProvider {

  /**
   * Creates an ApplicationProvider that wraps an Application instance.
   */
  def apply(application: Application) = new ApplicationProvider {
    val get: Try[Application] = Success(application)
  }
}
