/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
    case t: RequestTaggingHandler =>
      // Call the RequestTaggingHandler logic on this request. This handler
      // will change the request header, but not the handler itself. Since the
      // handler hasn't been changed we don't need to call
      // `applyAllModifications` again. This means RequestTaggingHandlers can
      // only be one level deep; they do not compose.
      val newRequestHeader = t.tagRequest(requestHeader)
      (newRequestHeader, handler)
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

/**
 * A handler that is able to tag requests. Usually mixed in to other handlers.
 *
 * Instead of using the handler you should use [[Handler.Stage]].
 * `Handler.Stage` is a handler improves upon the `RequestTaggingHandler` in several ways:
 * (a) `Handler.Stage` can be nested to arbitrary depth, (b) it doesn't require
 * mixing-in and (c) it allows handlers to be rewritten as well as requests, (d) it
 * prevents Play from accessing the real handler until its logic has been run.
 */
@deprecated("Use Handler.Stage instead", "2.6.0")
trait RequestTaggingHandler extends Handler {
  def tagRequest(request: RequestHeader): RequestHeader
}
