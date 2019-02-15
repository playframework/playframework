/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import akka.annotation.ApiMayChange

/**
 * Wrapper for a sequence of [[ServerEndpoint]]s. Has a few convenience methods. Also
 * can be used as an implicit parameter to pass around server endpoint information.
 */
@ApiMayChange final case class ServerEndpoints(endpoints: Seq[ServerEndpoint]) {
  private def endpointForScheme(scheme: String): Option[ServerEndpoint] =
    endpoints.find(_.scheme == scheme)

  /** Convenient way to get an HTTP endpoint */
  val httpEndpoint: Option[ServerEndpoint] = endpointForScheme("http")

  /** Convenient way to get an HTTPS endpoint */
  val httpsEndpoint: Option[ServerEndpoint] = endpointForScheme("https")
}
