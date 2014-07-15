/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import java.io.File
import java.util.Properties
import play.api.Mode

/**
 * Common configuration for servers such as NettyServer.
 */
case class ServerConfig(
    rootDir: File,
    port: Option[Int],
    sslPort: Option[Int] = None,
    address: String = "0.0.0.0",
    mode: Mode.Mode = Mode.Prod,
    properties: Properties) {
  // Some basic validation of config
  if (!port.isDefined && !sslPort.isDefined) throw new IllegalArgumentException("Must provide either an HTTP port or an HTTPS port")
}
