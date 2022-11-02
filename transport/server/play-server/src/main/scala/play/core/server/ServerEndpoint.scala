/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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

  /**
   * Create a full WebSocket URL out of a path. E.g. a path of `/foo` becomes `ws://localhost:12345/foo`
   */
  def wsPathUrl(path: String): String = {
    val wsScheme = scheme match {
      case "http"  => "ws"
      case "https" => "wss"
    }
    s"$wsScheme://$host:$port$path"
  }
}
