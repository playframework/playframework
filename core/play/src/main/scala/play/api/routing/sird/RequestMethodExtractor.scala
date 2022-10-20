/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
  val GET = new RequestMethodExtractor("GET")

  /**
   * Extracts a POST request.
   */
  val POST = new RequestMethodExtractor("POST")

  /**
   * Extracts a PUT request.
   */
  val PUT = new RequestMethodExtractor("PUT")

  /**
   * Extracts a DELETE request.
   */
  val DELETE = new RequestMethodExtractor("DELETE")

  /**
   * Extracts a PATCH request.
   */
  val PATCH = new RequestMethodExtractor("PATCH")

  /**
   * Extracts an OPTIONS request.
   */
  val OPTIONS = new RequestMethodExtractor("OPTIONS")

  /**
   * Extracts a HEAD request.
   */
  val HEAD = new RequestMethodExtractor("HEAD")
}
