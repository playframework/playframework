/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import javax.net.ssl._

import akka.annotation.ApiMayChange

import play.core.server.ServerEndpoint.ClientSsl

/**
 * Contains information about which port and protocol can be used to connect to the server.
 * This class is used to abstract out the details of connecting to different backends
 * and protocols. Most tests will operate the same no matter which endpoint they
 * are connected to.
 */
@ApiMayChange final case class ServerEndpoint(
    description: String,
    scheme: String,
    host: String,
    port: Int,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String],
    ssl: Option[ClientSsl]
) {

  /**
   * Create a full URL out of a path. E.g. a path of `/foo` becomes `http://localhost:12345/foo`
   */
  def pathUrl(path: String): String = s"$scheme://$host:$port$path"

}

@ApiMayChange object ServerEndpoint {
  /** Contains SSL information for a client that wants to connect to a [[ServerEndpoint]]. */
  final case class ClientSsl(sslContext: SSLContext, trustManager: X509TrustManager)
}
