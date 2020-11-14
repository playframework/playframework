/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import javax.net.ssl._

import akka.annotation.ApiMayChange

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
    protocols: Set[String],
    serverAttribute: Option[String],
    ssl: Option[SSLContext]
) {

  /**
   * Create a full URL out of a path. E.g. a path of `/foo` becomes `http://localhost:12345/foo`
   */
  def pathUrl(path: String): String = s"$scheme://$host:$port$path"
}
