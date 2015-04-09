/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import java.io._
import play.utils.Threads

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

import play.api._
import play.api.mvc._
import scala.util.control.NonFatal

/**
 * provides source code to be displayed on error pages
 */
trait SourceMapper {

  def sourceOf(className: String, line: Option[Int] = None): Option[(File, Option[Int])]

  def sourceFor(e: Throwable): Option[(File, Option[Int])] = {
    e.getStackTrace.find(element => sourceOf(element.getClassName).isDefined).flatMap { interestingStackTrace =>
      sourceOf(interestingStackTrace.getClassName, Option(interestingStackTrace.getLineNumber))
    }
  }

}

/**
 * Provides information about a Play Application running inside a Play server.
 */
trait ApplicationProvider {

  /**
   * Get the application. In dev mode this lazily loads the application.
   */
  def get: Try[Application]

  /**
   * Get the currently loaded application. May be empty in dev mode because of compile failure or before first load.
   */
  def current: Option[Application] = get.toOption

  /**
   * Handle a request directly, without using the application.
   */
  def handleWebCommand(requestHeader: play.api.mvc.RequestHeader): Option[Result] = None
}

object ApplicationProvider {

  /**
   * Creates an ApplicationProvider that wraps an Application instance.
   */
  def apply(application: Application) = new ApplicationProvider {
    val get: Try[Application] = Success(application)
  }

}

trait HandleWebCommandSupport {
  def handleWebCommand(request: play.api.mvc.RequestHeader, buildLink: play.core.BuildLink, path: java.io.File): Option[Result]
}