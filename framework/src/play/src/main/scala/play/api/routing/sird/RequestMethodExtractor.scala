/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.routing.sird

import play.api.mvc.RequestHeader

/**
 * An extractor that extracts requests by method.
 */
class RequestMethodExtractor private[sird] (method: String) {
  def unapply(request: RequestHeader): Option[RequestHeader] =
    Some(request).filter(_.method.equalsIgnoreCase(method))
}

/**
 * Extractors that extract requests by method.
 */
trait RequestMethodExtractors {

  /**
   * Extracts a GET request.
   */
  final val GET = new RequestMethodExtractor("GET")

  /**
   * Extracts a POST request.
   */
  final val POST = new RequestMethodExtractor("POST")

  /**
   * Extracts a PUT request.
   */
  final val PUT = new RequestMethodExtractor("PUT")

  /**
   * Extracts a DELETE request.
   */
  final val DELETE = new RequestMethodExtractor("DELETE")

  /**
   * Extracts a PATCH request.
   */
  final val PATCH = new RequestMethodExtractor("PATCH")

  /**
   * Extracts an OPTIONS request.
   */
  final val OPTIONS = new RequestMethodExtractor("OPTIONS")

  /**
   * Extracts a HEAD request.
   */
  final val HEAD = new RequestMethodExtractor("HEAD")
}
