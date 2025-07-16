/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import javax.inject.Inject

import play.api.http.HttpConfiguration
import play.api.libs.crypto.CookieSignerProvider
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.core.system.RequestIdProvider

/**
 * A `RequestFactory` provides logic for creating requests.
 */
trait RequestFactory {

  /**
   * Create a `RequestHeader`.
   */
  def createRequestHeader(
      connection: RemoteConnection,
      method: String,
      target: RequestTarget,
      version: String,
      headers: Headers,
      attrs: TypedMap
  ): RequestHeader

  /**
   * Creates a `RequestHeader` based on the values of an
   * existing `RequestHeader`. The factory may modify the copied
   * values to produce a modified `RequestHeader`.
   */
  def copyRequestHeader(rh: RequestHeader): RequestHeader = {
    createRequestHeader(rh.connection, rh.method, rh.target, rh.version, rh.headers, rh.attrs)
  }

  /**
   * Create a `Request` with a body. By default this just calls
   * `createRequestHeader(...).withBody(body)`.
   */
  def createRequest[A](
      connection: RemoteConnection,
      method: String,
      target: RequestTarget,
      version: String,
      headers: Headers,
      attrs: TypedMap,
      body: A
  ): Request[A] =
    createRequestHeader(connection, method, target, version, headers, attrs).withBody(body)

  /**
   * Creates a `Request` based on the values of an
   * existing `Request`. The factory may modify the copied
   * values to produce a modified `Request`.
   */
  def copyRequest[A](r: Request[A]): Request[A] = {
    createRequest[A](r.connection, r.method, r.target, r.version, r.headers, r.attrs, r.body)
  }
}

object RequestFactory {

  /**
   * A `RequestFactory` that creates a request with the arguments given, without
   * any additional modification.
   */
  val plain = new RequestFactory {
    override def createRequestHeader(
        connection: RemoteConnection,
        method: String,
        target: RequestTarget,
        version: String,
        headers: Headers,
        attrs: TypedMap
    ): RequestHeader =
      new RequestHeaderImpl(connection, method, target, version, headers, attrs)
  }
}

/**
 * The default [[RequestFactory]] used by a Play application. This
 * `RequestFactory` adds the following typed attributes to requests:
 * - request id (if not existing yet)
 * - cookie
 * - session cookie
 * - flash cookie
 */
class DefaultRequestFactory @Inject() (
    val cookieHeaderEncoding: CookieHeaderEncoding,
    val sessionBaker: SessionCookieBaker,
    val flashBaker: FlashCookieBaker
) extends RequestFactory {
  def this(config: HttpConfiguration) = this(
    new DefaultCookieHeaderEncoding(config.cookies),
    new DefaultSessionCookieBaker(config.session, config.secret, new CookieSignerProvider(config.secret).get),
    new DefaultFlashCookieBaker(config.flash, config.secret, new CookieSignerProvider(config.secret).get)
  )

  override def createRequestHeader(
      connection: RemoteConnection,
      method: String,
      target: RequestTarget,
      version: String,
      headers: Headers,
      attrs: TypedMap
  ): RequestHeader = {
    // Generate a new request ID only if one has not already been generated at an earlier stage
    val requestId: Long = attrs.get(RequestAttrKey.Id).getOrElse(RequestIdProvider.freshId())
    val cookieCell      = new LazyCell[Cookies] {
      protected override def emptyMarker: Cookies = null
      protected override def create: Cookies      =
        cookieHeaderEncoding.fromCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE))
    }
    val sessionCell = new LazyCell[Session] {
      protected override def emptyMarker: Session = null
      protected override def create: Session      =
        sessionBaker.decodeFromCookie(cookieCell.value.get(sessionBaker.COOKIE_NAME))
    }
    val flashCell = new LazyCell[Flash] {
      protected override def emptyMarker: Flash = null
      protected override def create: Flash      = flashBaker.decodeFromCookie(cookieCell.value.get(flashBaker.COOKIE_NAME))
    }
    val updatedAttrMap = attrs.updated(
      RequestAttrKey.Id      -> requestId,
      RequestAttrKey.Cookies -> cookieCell,
      RequestAttrKey.Session -> sessionCell,
      RequestAttrKey.Flash   -> flashCell
    )
    new RequestHeaderImpl(connection, method, target, version, headers, updatedAttrMap)
  }
}
