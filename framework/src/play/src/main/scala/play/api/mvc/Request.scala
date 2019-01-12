/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.Locale

import play.api.i18n.{ Lang, Messages }
import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.mvc.Http

import scala.annotation.{ implicitNotFound, tailrec }

/**
 * The complete HTTP request.
 *
 * @tparam A the body content type.
 */
@implicitNotFound("Cannot find any HTTP Request here")
trait Request[+A] extends RequestHeader {
  self =>

  /**
   * True if this request has a body. This is either done by inspecting the body itself to see if it is an entity
   * representing an "empty" body.
   */
  override def hasBody: Boolean = {
    @tailrec @inline def isEmptyBody(body: Any): Boolean = body match {
      case rb: play.mvc.Http.RequestBody => isEmptyBody(rb.as(classOf[AnyRef]))
      case AnyContentAsEmpty | null | Unit => true
      case unit if unit.isInstanceOf[scala.runtime.BoxedUnit] => true
      case _ => false
    }
    !isEmptyBody(body) || super.hasBody
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
  override def withConnection(newConnection: RemoteConnection): Request[A] =
    new RequestImpl[A](newConnection, method, target, version, headers, attrs, body)
  override def withMethod(newMethod: String): Request[A] =
    new RequestImpl[A](connection, newMethod, target, version, headers, attrs, body)
  override def withTarget(newTarget: RequestTarget): Request[A] =
    new RequestImpl[A](connection, method, newTarget, version, headers, attrs, body)
  override def withVersion(newVersion: String): Request[A] =
    new RequestImpl[A](connection, method, target, newVersion, headers, attrs, body)
  override def withHeaders(newHeaders: Headers): Request[A] =
    new RequestImpl[A](connection, method, target, version, newHeaders, attrs, body)
  override def withAttrs(newAttrs: TypedMap): Request[A] =
    new RequestImpl[A](connection, method, target, version, headers, newAttrs, body)
  override def addAttr[B](key: TypedKey[B], value: B): Request[A] =
    withAttrs(attrs.updated(key, value))
  override def removeAttr(key: TypedKey[_]): Request[A] =
    withAttrs(attrs - key)
  override def withTransientLang(lang: Lang): Request[A] =
    addAttr(Messages.Attrs.CurrentLang, lang)
  override def withTransientLang(code: String): Request[A] =
    withTransientLang(Lang(code))
  override def withTransientLang(locale: Locale): Request[A] =
    withTransientLang(Lang(locale))
  override def withoutTransientLang(): Request[A] =
    removeAttr(Messages.Attrs.CurrentLang)

  override def asJava: Http.Request = this match {
    case req: Request[Http.RequestBody] =>
      // This will preserve the parsed body since it is already using the Java body wrapper
      new Http.RequestImpl(req)
    case _ =>
      new Http.RequestImpl(this)
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
    override val connection: RemoteConnection,
    override val method: String,
    override val target: RequestTarget,
    override val version: String,
    override val headers: Headers,
    override val attrs: TypedMap,
    override val body: A) extends Request[A]
