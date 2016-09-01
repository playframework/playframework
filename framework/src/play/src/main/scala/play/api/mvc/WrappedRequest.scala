/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import play.api.libs.typedmap.{ TypedEntry, TypedKey }

/**
 * Wrap an existing request. Useful to extend a request.
 *
 * If you need to add extra values to a request, you could consider
 * using request attributes instead. See the `attr`, `withAttr`, etc
 * methods.
 */
class WrappedRequest[+A](request: Request[A]) extends Request[A] {
  override def id = request.id
  override def tags = request.tags
  override def body = request.body
  override def headers = request.headers
  override def queryString = request.queryString
  override def path = request.path
  override def uri = request.uri
  override def method = request.method
  override def version = request.version
  override def remoteAddress = request.remoteAddress
  override def secure = request.secure
  override def clientCertificateChain = request.clientCertificateChain

  /**
   * Create a copy of this wrapper, but wrapping a new request.
   * Subclasses can override this method.
   */
  protected def newWrapper[B](newRequest: Request[B]): WrappedRequest[B] = new WrappedRequest[B](newRequest)

  override def withBody[B](body: B): WrappedRequest[B] = newWrapper(request.withBody(body))
  override def withAttr[B](key: TypedKey[B], value: B): WrappedRequest[A] = newWrapper(request.withAttr(key, value))
  override def withAttrs(entries: TypedEntry[_]*): WrappedRequest[A] = newWrapper(request.withAttrs(entries: _*))
  override def getAttr[A](key: TypedKey[A]): Option[A] = request.getAttr(key)
  override def attr[A](key: TypedKey[A]): A = request.attr(key)
  override def containsAttr(key: TypedKey[_]): Boolean = request.containsAttr(key)
}
