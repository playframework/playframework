/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.URI

/**
 * The target of a request, as defined in RFC 7230 section 5.3, i.e. the URI or path that has been requested
 * by the client.
 */
trait RequestTarget {
  top =>

  /**
   * The parsed URI of the request. In rare circumstances, the URI may be unparseable
   * and accessing this value will throw an exception.
   */
  def uri: URI

  /**
   * The complete request URI, containing both path and query string.
   * The URI is what was on the status line after the request method.
   * E.g. in "GET /foo/bar?q=s HTTP/1.1" the URI should be /foo/bar?q=s.
   * It could be absolute, some clients send absolute URLs, especially proxies,
   * e.g. http://www.example.org/foo/bar?q=s.
   */
  def uriString: String

  /**
   * The path that was requested. If a URI was provided this will be its path component.
   */
  def path: String

  /**
   * The query component of the URI parsed into a map of parameters and values.
   */
  def queryMap: Map[String, Seq[String]]

  /**
   * The query component of the URI as an unparsed string.
   */
  def queryString: String = uriString.split('?').drop(1).mkString("?")

  /**
   * Helper method to access a query parameter.
   *
   * @return The query parameter's value if the parameter is present
   *         and there is only one value. If the parameter is absent
   *         or there is more than one value for that parameter then
   *         `None` is returned.
   */
  def getQueryParameter(key: String): Option[String] = queryMap.get(key).flatMap(_.headOption)

  /**
   * Return a copy of this object with a new URI.
   */
  def withUri(newUri: URI): RequestTarget = new RequestTarget {
    override def uri: URI = newUri
    override def uriString: String = newUri.toString
    override def queryMap: Map[String, Seq[String]] = top.queryMap
    override def path: String = top.path
  }
  /**
   * Return a copy of this object with a new URI.
   */
  def withUriString(newUriString: String): RequestTarget = new RequestTarget {
    override lazy val uri: URI = new URI(newUriString)
    override def uriString: String = newUriString
    override def queryMap: Map[String, Seq[String]] = top.queryMap
    override def path: String = top.path
  }

  /**
   * Return a copy of this object with a new path.
   */
  def withPath(newPath: String): RequestTarget = new RequestTarget {
    override def uri: URI = top.uri
    override def uriString: String = top.uriString
    override def queryMap: Map[String, Seq[String]] = top.queryMap
    override def path: String = newPath
  }

  /**
   * Return a copy of this object with a new query string.
   */
  def withQueryString(newQueryString: Map[String, Seq[String]]): RequestTarget = new RequestTarget {
    override def uri: URI = top.uri
    override def uriString: String = top.uriString
    override def queryMap: Map[String, Seq[String]] = newQueryString
    override def path: String = top.path
  }
}

object RequestTarget {

  /**
   * Create a new RequestTarget from the given values.
   */
  def apply(uriString: String, path: String, queryString: Map[String, Seq[String]]): RequestTarget = {
    val us = uriString
    val p = path
    val qs = queryString
    new RequestTarget {
      override lazy val uri: URI = new URI(us)
      override val uriString: String = us
      override val path: String = p
      override val queryMap: Map[String, Seq[String]] = qs
    }
  }
}