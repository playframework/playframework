/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.Scheme
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.XForwardedClientCert

/**
 * Wrap an existing request. Useful to extend a request.
 *
 * If you need to add extra values to a request, you could consider
 * using request attributes instead. See the `attr`, `withAttr`, etc
 * methods.
 */
class WrappedRequest[+A](request: Request[A]) extends Request[A] {
  override def transport: TransportConnection                             = request.transport
  override def clientCertificate: Option[ClientCertificateInfo]           = request.clientCertificate
  override def xForwardedClientCertificates: Vector[XForwardedClientCert] = request.xForwardedClientCertificates
  override def scheme: Scheme                                             = request.scheme
  override def authority: Option[RequestAuthority]                        = request.authority
  override def remote: RemoteInfo                                         = request.remote
  override def method: String                                             = request.method
  override def target: RequestTarget                                      = request.target
  override def version: String                                            = request.version
  override def headers: Headers                                           = request.headers
  override def body: A                                                    = request.body
  override def attrs: TypedMap                                            = request.attrs

  /**
   * Create a copy of this wrapper, but wrapping a new request.
   * Subclasses can override this method.
   */
  protected def newWrapper[B](newRequest: Request[B]): WrappedRequest[B] =
    new WrappedRequest[B](newRequest)

  override def withRemote(newRemote: RemoteInfo): WrappedRequest[A] =
    newWrapper(request.withRemote(newRemote))
  override def withTransport(newTransport: TransportConnection): WrappedRequest[A] =
    newWrapper(request.withTransport(newTransport))
  override def withClientCertificate(newClientCertificate: Option[ClientCertificateInfo]): WrappedRequest[A] =
    newWrapper(request.withClientCertificate(newClientCertificate))
  override def withXForwardedClientCertificates(
      newXForwardedClientCertificates: Seq[XForwardedClientCert]
  ): WrappedRequest[A] =
    newWrapper(request.withXForwardedClientCertificates(newXForwardedClientCertificates))
  override def withScheme(newScheme: Scheme): WrappedRequest[A] =
    newWrapper(request.withScheme(newScheme))
  override def withAuthority(newAuthority: Option[RequestAuthority]): WrappedRequest[A] =
    newWrapper(request.withAuthority(newAuthority))
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
