/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.security.cert.X509Certificate

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
  def map[B](f: A => B): Request[B] = new Request[B] {
    override def id = self.id
    override def tags = self.tags
    override def uri = self.uri
    override def path = self.path
    override def method = self.method
    override def version = self.version
    override def queryString = self.queryString
    override def headers = self.headers
    override def remoteAddress = self.remoteAddress
    override def secure = self.secure
    override def clientCertificateChain = self.clientCertificateChain

    override lazy val body = f(self.body)
  }

}

object Request {

  def apply[A](rh: RequestHeader, a: A): Request[A] = new Request[A] {
    override def id = rh.id
    override def tags = rh.tags
    override def uri = rh.uri
    override def path = rh.path
    override def method = rh.method
    override def version = rh.version
    override def queryString = rh.queryString
    override def headers = rh.headers
    override lazy val remoteAddress = rh.remoteAddress
    override lazy val secure = rh.secure
    override val clientCertificateChain = rh.clientCertificateChain
    override val body = a
  }
}

/** Used by Java wrapper */
private[play] class RequestImpl[A](
    override val body: A,
    override val id: Long,
    override val tags: Map[String, String],
    override val uri: String,
    override val path: String,
    override val method: String,
    override val version: String,
    override val queryString: Map[String, Seq[String]],
    override val headers: Headers,
    override val remoteAddress: String,
    override val secure: Boolean,
    override val clientCertificateChain: Option[Seq[X509Certificate]]) extends Request[A] {
}