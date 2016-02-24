/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import java.io.File
import java.util.Properties
import play.api.{ Configuration, Mode }

/**
 * Common configuration for servers such as NettyServer.
 *
 * @param rootDir The root directory of the server. Used to find default locations of
 * files, log directories, etc.
 * @param port The HTTP port to use.
 * @param sslPort The HTTPS port to use.
 * @param address The socket address to bind to.
 * @param mode The run mode: dev, test or prod.
 * @param configuration: The configuration to use for loading the server. This is not
 * the same as application configuration. This configuration is usually loaded from a
 * server.conf file, whereas the application configuration is usually loaded from an
 * application.conf file.
 */
case class ServerConfig(
    rootDir: File,
    port: Option[Int],
    sslPort: Option[Int],
    address: String,
    mode: Mode.Mode,
    properties: Properties,
    configuration: Configuration) {
  // Some basic validation of config
  if (!port.isDefined && !sslPort.isDefined) throw new IllegalArgumentException("Must provide either an HTTP port or an HTTPS port")
}

object ServerConfig {

  def apply(
    classLoader: ClassLoader = this.getClass.getClassLoader,
    rootDir: File = new File("."),
    port: Option[Int] = Some(9000),
    sslPort: Option[Int] = None,
    address: String = "0.0.0.0",
    mode: Mode.Mode = Mode.Prod,
    properties: Properties = System.getProperties): ServerConfig = {
    ServerConfig(
      rootDir = rootDir,
      port = port,
      sslPort = sslPort,
      address = address,
      mode = mode,
      properties = properties,
      configuration = Configuration.load(classLoader, properties, rootDirConfig(rootDir), mode == Mode.Test)
    )
  }

  /**
   * Gets the configuration for the given root directory. Used to construct
   * the server Configuration.
   */
  def rootDirConfig(rootDir: File): Map[String, String] =
    Map("play.server.dir" -> rootDir.getAbsolutePath)

}
