/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.Locale
import java.util.Optional

import scala.annotation.implicitNotFound
import scala.annotation.tailrec

import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.libs.typedmap.TypedEntry
import play.api.libs.typedmap.TypedKey
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.Scheme
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.XForwardedClientCert
import play.mvc.Http

/**
 * The complete HTTP request.
 *
 * @tparam A the body content type.
 */
@implicitNotFound("Cannot find any HTTP Request here")
trait Request[+A] extends RequestHeader {
  self =>

  /**
   * True if this request has a body. This is either done by inspecting the request headers or the body itself to see if
   * it is an entity representing an "empty" body.
   */
  override def hasBody: Boolean = {
    import play.api.http.HeaderNames._
    if (headers.get(CONTENT_LENGTH).isDefined || headers.get(TRANSFER_ENCODING).isDefined) {
      // A relevant header is set, which means this is a real request or a fake request used for testing where the user
      // cared about setting the headers. We can just use them to see if a body exists. In a real life production application,
      // where clients basically always send these headers when applicable (for requests that send bodies like POST, etc.)
      // we are very likely to enter this if branch.
      super.hasBody
    } else {
      // No relevant header present, very likely this is a real life GET request (or alike) without a body or a fake request
      // used for testing where the user did not care about setting the headers (but maybe did set an entity though).
      // Let's do our best to find out if there is an entity that represents an "empty" body.
      @tailrec @inline def isEmptyBody(body: Any): Boolean = body match {
        case rb: play.mvc.Http.RequestBody =>
          rb match {
            // In PlayJava, Optional.empty() is used to represent an empty body
            case _ if rb.as(classOf[Optional[?]]) != null => !rb.as(classOf[Optional[?]]).isPresent
            case _                                        => isEmptyBody(rb.as(classOf[AnyRef]))
          }
        case AnyContentAsEmpty | null | ()                      => true
        case unit if unit.isInstanceOf[scala.runtime.BoxedUnit] => true
        // All values which are known to represent an empty body have been checked, therefore, if we end up here, technically
        // it is sure something is set (at least it's not null), even though this something might represent "empty"/"no body"
        // (like an empty string or an empty ByteString) - but how should we know? This something could be a custom type
        // coming from a custom body parser defined entirely by the user... Sure, we could check for the most common types
        // if they represent an empty body (empty Strings, empty ByteString, etc.) but that would not be consistent
        // (custom types defined by the user that represent "empty" would still return false)
        case _ => false
      }

      !isEmptyBody(body)
    }
  }

  /**
   * The body content.
   */
  def body: A

  /**
   * Transform the request body.
   */
  def map[B](f: A => B): Request[B] = withBody(f(body))

  // Override the return type and default implementation of these RequestHeader methods
  override def withTransport(newTransport: TransportConnection): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      newTransport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withClientCertificate(newClientCertificate: Option[ClientCertificateInfo]): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      newClientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withXForwardedClientCertificates(
      newXForwardedClientCertificates: Seq[XForwardedClientCert]
  ): Request[A] = {
    require(newXForwardedClientCertificates != null, "The XFCC assertion sequence must not be null")
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      newXForwardedClientCertificates.toVector
    )
  }
  override def withScheme(newScheme: Scheme): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      newScheme,
      authority,
      xForwardedClientCertificates
    )
  override def withAuthority(newAuthority: Option[RequestAuthority]): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      newAuthority,
      xForwardedClientCertificates
    )
  override def withRemote(newRemote: RemoteInfo): Request[A] =
    new RequestImpl[A](
      newRemote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withMethod(newMethod: String): Request[A] =
    new RequestImpl[A](
      remote,
      newMethod,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withTarget(newTarget: RequestTarget): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      newTarget,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withVersion(newVersion: String): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      newVersion,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withHeaders(newHeaders: Headers): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      RequestHeader.validateReplacementHeaders(newHeaders, authority),
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def withAttrs(newAttrs: TypedMap): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      newAttrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )
  override def addAttr[B](key: TypedKey[B], value: B): Request[A] =
    withAttrs(attrs.updated(key, value))
  override def addAttrs(e1: TypedEntry[?]): Request[A]                                       = withAttrs(attrs.updated(e1))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?]): Request[A]                    = withAttrs(attrs.updated(e1, e2))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): Request[A] =
    withAttrs(attrs.updated(e1, e2, e3))
  override def addAttrs(entries: TypedEntry[?]*): Request[A] =
    withAttrs(attrs.updated(entries*))
  override def removeAttr(key: TypedKey[?]): Request[A] =
    withAttrs(attrs.removed(key))
  override def withTransientLang(lang: Lang): Request[A] =
    addAttr(Messages.Attrs.CurrentLang, lang)
  override def withTransientLang(code: String): Request[A] =
    withTransientLang(Lang(code))
  override def withTransientLang(locale: Locale): Request[A] =
    withTransientLang(Lang(locale))
  override def withoutTransientLang(): Request[A] =
    removeAttr(Messages.Attrs.CurrentLang)

  /**
   * Be aware that when converting a Scala request to a Java request that the body
   * will not be converted automatically to a Java equivalent body. For example:
   * If the Scala request contains a play.api.mvc.RawBuffer it will not be converted into it's Java equivalent
   * play.mvc.Http.RawBuffer, or a Scala AnyContentAsEmpty will not be converted into a java.util.Optional.empty()
   * (which is the Play Java equivalent of an empty body). Therefore helper methods like request.asJava.body().asRaw(),
   * asJson(), etc. will very likely not work. You can however retrieve any stored body object by using
   * request.asJava.body().as(classOf[Object]).
   */
  override def asJava: Http.Request = this.body match {
    case null =>
      new Http.RequestImpl(this.withBody(null))
    case rb: Http.RequestBody => // This will preserve the parsed body since it is already using the Java body wrapper
      new Http.RequestImpl(this.withBody(rb))
    case rb =>
      new Http.RequestImpl(this.withBody(new Http.RequestBody(rb)))
  }
}

object Request {

  /**
   * Create a new Request from a RequestHeader and a body. The RequestHeader's
   * methods aren't evaluated when this method is called.
   */
  def apply[A](rh: RequestHeader, body: A): Request[A] = rh.withBody(body)
}

/**
 * A standard implementation of a Request.
 *
 * @param body The body of the request.
 * @tparam A The type of the body content.
 */
private[play] class RequestImpl[+A](
    override val remote: RemoteInfo,
    override val method: String,
    override val target: RequestTarget,
    override val version: String,
    requestHeaders: Headers,
    override val attrs: TypedMap,
    override val body: A,
    override val transport: TransportConnection,
    override val clientCertificate: Option[ClientCertificateInfo],
    override val scheme: Scheme,
    override val authority: Option[RequestAuthority],
    override val xForwardedClientCertificates: Vector[XForwardedClientCert] = Vector.empty
) extends Request[A] {
  require(
    clientCertificate != null && clientCertificate.forall(_ != null),
    "Effective client certificate option must not be null or contain null"
  )
  require(
    xForwardedClientCertificates != null && xForwardedClientCertificates.forall(_ != null),
    "The XFCC assertion sequence must not be null or contain null"
  )
  override val headers: Headers = RequestHeader.canonicalHeaders(requestHeaders, authority)
}
