/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.Locale
import java.util.Optional

import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.libs.typedmap.TypedKey
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.mvc.Http

import scala.annotation.implicitNotFound
import scala.annotation.tailrec

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
            case _ if rb.as(classOf[Optional[_]]) != null => !rb.as(classOf[Optional[_]]).isPresent
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
    case req: Request[Http.RequestBody @unchecked] =>
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
    override val body: A
) extends Request[A]
