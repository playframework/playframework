/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import play.api.{ Application, Configuration }
import play.core.ApplicationProvider

/**
 * An object that knows how to obtain a server. Instantiating a
 * ServerProvider object should be fast and side-effect free. Any
 * actual work that a ServerProvider needs to do should be delayed
 * until the `createServer` method is called.
 */
trait ServerProvider {
  def createServer(config: ServerConfig, appProvider: ApplicationProvider): Server
  final def createServer(config: ServerConfig, app: Application): Server = createServer(config, ApplicationProvider(app))
}

object ServerProvider {

  /**
   * Load a server provider from the configuration and classloader.
   *
   * @param classLoader The ClassLoader to load the class from.
   * @param configuration The configuration to look the server provider up from.
   * @return The server provider, if one was configured.
   * @throws ServerStartException If the ServerProvider couldn't be created.
   */
  def fromConfiguration(classLoader: ClassLoader, configuration: Configuration): ServerProvider = {
    val ClassNameConfigKey = "play.server.provider"
    val className: String = configuration.getString(ClassNameConfigKey).getOrElse(throw new ServerStartException(s"No ServerProvider configured with key '$ClassNameConfigKey'"))
    val clazz = try classLoader.loadClass(className) catch {
      case _: ClassNotFoundException => throw ServerStartException(s"Couldn't find ServerProvider class '$className'")
    }
    if (!classOf[ServerProvider].isAssignableFrom(clazz)) throw ServerStartException(s"Class ${clazz.getName} must implement ServerProvider interface")
    val ctor = try clazz.getConstructor() catch {
      case _: NoSuchMethodException => throw ServerStartException(s"ServerProvider class ${clazz.getName} must have a public default constructor")
    }
    ctor.newInstance().asInstanceOf[ServerProvider]
  }

  /**
   * Load the default server provider.
   */
  implicit lazy val defaultServerProvider: ServerProvider = {
    val classLoader = this.getClass.getClassLoader
    val config = Configuration.load(classLoader, System.getProperties, Map.empty, true)
    fromConfiguration(classLoader, config)
  }

}