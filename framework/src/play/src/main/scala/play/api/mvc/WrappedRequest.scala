/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

/**
 * Wrap an existing request. Useful to extend a request.
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
}
