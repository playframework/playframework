/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.io.File
import javax.inject.Singleton

import scala.collection.mutable.ArrayBuffer

import play.api.mvc.RequestHeader
import play.api.mvc.Result

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
    handlers.to(LazyList).flatMap { _.handleWebCommand(request, buildLink, path).to(Seq) }.headOption
  }
}
