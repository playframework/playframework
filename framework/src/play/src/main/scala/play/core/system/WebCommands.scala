/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import java.io.File
import javax.inject.Singleton
import play.api.mvc.{ Result, RequestHeader }
import scala.collection.mutable.ArrayBuffer

/**
 * Handlers for web commands.
 */
trait WebCommands {

  /**
   * Add a handler to be called on ApplicationProvider.handleWebCommand.
   */
  def addHandler(handler: HandleWebCommandSupport): Unit

  /**
   * Call handleWebCommand on the handlers.
   * @return the result from the first Some-returning handler
   */
  def handleWebCommand(request: RequestHeader, buildLink: BuildLink, path: File): Option[Result]
}

/**
 * Default implementation of web commands.
 */
@Singleton
class DefaultWebCommands extends WebCommands {
  private[this] val handlers = ArrayBuffer.empty[HandleWebCommandSupport]

  def addHandler(handler: HandleWebCommandSupport): Unit = synchronized {
    handlers += handler
  }

  def handleWebCommand(request: RequestHeader, buildLink: BuildLink, path: File): Option[Result] = synchronized {
    (handlers.toStream flatMap { _.handleWebCommand(request, buildLink, path).toSeq }).headOption
  }
}
