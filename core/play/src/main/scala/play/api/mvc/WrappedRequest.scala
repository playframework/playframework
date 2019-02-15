/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{ RemoteConnection, RequestTarget }

/**
 * Wrap an existing request. Useful to extend a request.
 *
 * If you need to add extra values to a request, you could consider
 * using request attributes instead. See the `attr`, `withAttr`, etc
 * methods.
 */
class WrappedRequest[+A](request: Request[A]) extends Request[A] {
  override def connection: RemoteConnection = request.connection
  override def method: String = request.method
  override def target: RequestTarget = request.target
  override def version: String = request.version
  override def headers: Headers = request.headers
  override def body: A = request.body
  override def attrs: TypedMap = request.attrs

  /**
   * Create a copy of this wrapper, but wrapping a new request.
   * Subclasses can override this method.
   */
  protected def newWrapper[B](newRequest: Request[B]): WrappedRequest[B] =
    new WrappedRequest[B](newRequest)

  override def withConnection(newConnection: RemoteConnection): WrappedRequest[A] =
    newWrapper(request.withConnection(newConnection))
  override def withMethod(newMethod: String): WrappedRequest[A] =
    newWrapper(request.withMethod(newMethod))
  override def withTarget(newTarget: RequestTarget): WrappedRequest[A] =
    newWrapper(request.withTarget(newTarget))
  override def withVersion(newVersion: String): WrappedRequest[A] =
    newWrapper(request.withVersion(newVersion))
  override def withHeaders(newHeaders: Headers): WrappedRequest[A] =
    newWrapper(request.withHeaders(newHeaders))
  override def withAttrs(newAttrs: TypedMap): WrappedRequest[A] =
    newWrapper(request.withAttrs(newAttrs))
  override def withBody[B](body: B): WrappedRequest[B] =
    newWrapper(request.withBody(body))
}
