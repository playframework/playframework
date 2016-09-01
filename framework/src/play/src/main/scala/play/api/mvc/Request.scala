/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.security.cert.X509Certificate

import play.api.libs.typedmap.{ TypedEntry, TypedKey, TypedMap }

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

  // Override the return type of these RequestHeader methods
  override def withAttr[T](key: TypedKey[T], value: T): Request[A]
  override def withAttrs(entries: TypedEntry[_]*): Request[A]

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
 * @param remoteAddressFunc A function that evaluates to the remote address.
 * @param secureFunc A function that evaluates to the security status.
 * @param attrMap A map of the request's typed attributes.
 * @param body The body of the request.
 * @tparam A The type of the body content.
 */
private[play] class RequestImpl[+A](
    override val id: Long,
    override val tags: Map[String, String],
    override val uri: String,
    override val path: String,
    override val method: String,
    override val version: String,
    override val queryString: Map[String, Seq[String]],
    override val headers: Headers,
    remoteAddressFunc: () => String,
    secureFunc: () => Boolean,
    override val clientCertificateChain: Option[Seq[X509Certificate]],
    override protected val attrMap: TypedMap,
    override val body: A) extends Request[A] with WithAttrMap[Request[A]] {

  def this(
    id: Long,
    tags: Map[String, String],
    uri: String,
    path: String,
    method: String,
    version: String,
    queryString: Map[String, Seq[String]],
    headers: Headers,
    remoteAddress: String,
    secure: Boolean,
    clientCertificateChain: Option[Seq[X509Certificate]],
    attrMap: TypedMap,
    body: A) = {
    this(
      id = id,
      tags = tags,
      uri = uri,
      path = path,
      method = method,
      version = version,
      queryString = queryString,
      headers = headers,
      remoteAddressFunc = () => remoteAddress,
      secureFunc = () => secure,
      clientCertificateChain = clientCertificateChain,
      attrMap = attrMap,
      body = body
    )
  }

  override lazy val remoteAddress: String = remoteAddressFunc()
  override lazy val secure: Boolean = secureFunc()

  override protected def withAttrMap(newAttrMap: TypedMap): Request[A] = {
    new RequestImpl[A](
      id = id,
      tags = tags,
      uri = uri,
      path = path,
      method = method,
      version = version,
      queryString = queryString,
      headers = headers,
      remoteAddressFunc = () => remoteAddress,
      secureFunc = () => secure,
      clientCertificateChain = clientCertificateChain,
      attrMap = newAttrMap,
      body = body
    )
  }

  override def withBody[B](newBody: B): Request[B] = new RequestImpl[B](
    id = id,
    tags = tags,
    uri = uri,
    path = path,
    method = method,
    version = version,
    queryString = queryString,
    headers = headers,
    remoteAddressFunc = () => remoteAddress,
    secureFunc = () => secure,
    clientCertificateChain = clientCertificateChain,
    attrMap = attrMap,
    body = newBody
  )
}