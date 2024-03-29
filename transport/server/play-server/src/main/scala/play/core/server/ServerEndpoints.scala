/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import org.apache.pekko.annotation.ApiMayChange

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

@ApiMayChange
object ServerEndpoints {
  val empty: ServerEndpoints = ServerEndpoints(Seq.empty)
}
