/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import scala.annotation.tailrec

/**
 * An Handler handles a request. Play understands several types of handlers,
 * for example `EssentialAction`s and `WebSocket`s.
 *
 * The `Handler` used to handle the request is controlled by `GlobalSetting`s's
 * `onRequestReceived` method. The default implementation of
 * `onRequestReceived` delegates to `onRouteRequest` which calls the default
 * `Router`.
 */
trait Handler

final object Handler {
  /**
   * Some handlers are built as a series of stages, with each stage returning
   * a new [[RequestHeader]] and another stage, until eventually a terminal
   * handler is returned. This method processes all stages in a handler, if any,
   * returning a terminal handler such as `EssentialAction` or `WebSocket`.
   *
   * @param requestHeader The current RequestHeader.
   * @param handler The input Handler.
   * @return The new RequestHeader and Handler.
   */
  @tailrec
  def applyStages(requestHeader: RequestHeader, handler: Handler): (RequestHeader, Handler) = handler match {
    case m: Stage =>
      // Call the ModifyRequest logic to get the new header and handler. The
      // new handler could have its own modifications to apply to the header
      // so we call `applyPreprocessingHandlers` recursively on the result.
      val (newRequestHeader, newHandler) = m.apply(requestHeader)
      applyStages(newRequestHeader, newHandler)
    case _ =>
      // This is a normal handler that doesn't do any preprocessing.
      (requestHeader, handler)
  }

  /**
   * A special type of [[play.api.mvc.Handler]] which allows custom logic to be inserted
   * during handling. A `Stage` accepts a `RequestHeader` then returns a new
   * `RequestHeader` along with the next `Handler` to use during request
   * handling. The next handler could be a terminal `Handler` like an
   * [[EssentialAction]], but it could also be another `Stage`. This means
   * it's possible to chains of `Stage`s that should each be executed in turn.
   * To automatically execute all `Stage`s you can call [[play.api.mvc.Handler.applyStages]].
   */
  trait Stage extends Handler {
    def apply(requestHeader: RequestHeader): (RequestHeader, Handler)
  }

  object Stage {
    /**
     * Create a `Stage` that modifies the request before calling the next handler.
     */
    def modifyRequest(modifyRequestFunc: RequestHeader => RequestHeader, wrappedHandler: Handler): Handler.Stage = new Stage {
      override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = (modifyRequestFunc(requestHeader), wrappedHandler)
    }
  }
}
