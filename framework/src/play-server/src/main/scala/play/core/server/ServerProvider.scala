/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.{ Application, Configuration }
import play.core.ApplicationProvider
import scala.concurrent.Future

/**
 * An object that knows how to obtain a server. Instantiating a
 * ServerProvider object should be fast and side-effect free. Any
 * actual work that a ServerProvider needs to do should be delayed
 * until the `createServer` method is called.
 */
trait ServerProvider {
  def createServer(context: ServerProvider.Context): Server

  /**
   * Create a server for a given application.
   */
  final def createServer(config: ServerConfig, app: Application): Server =
    createServer(ServerProvider.Context(config, ApplicationProvider(app), app.actorSystem, app.materializer, () => Future.successful(())))
}

object ServerProvider {

  /**
   * The context for creating a server. Passed to the `createServer` method.
   *
   * @param config Basic server configuration values.
   * @param appProvider An object which can be queried to get an Application.
   * @param actorSystem An ActorSystem that the server can use.
   * @param stopHook A function that should be called by the server when it stops.
   * This function can be used to close resources that are provided to the server.
   */
  final case class Context(
      config: ServerConfig,
      appProvider: ApplicationProvider,
      actorSystem: ActorSystem,
      materializer: Materializer,
      stopHook: () => Future[_])

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
    val className: String = configuration.getOptional[String](ClassNameConfigKey)
      .getOrElse(throw ServerStartException(s"No ServerProvider configured with key '$ClassNameConfigKey'"))

    val clazz = try classLoader.loadClass(className) catch {
      case ex: ClassNotFoundException => throw ServerStartException(s"Couldn't find ServerProvider class '$className'", cause = Some(ex))
    }

    if (!classOf[ServerProvider].isAssignableFrom(clazz)) throw ServerStartException(s"Class ${clazz.getName} must implement ServerProvider interface")
    val constructor = try clazz.getConstructor() catch {
      case ex: NoSuchMethodException => throw ServerStartException(s"ServerProvider class ${clazz.getName} must have a public default constructor", cause = Some(ex))
    }

    constructor.newInstance().asInstanceOf[ServerProvider]
  }

  /**
   * Load the default server provider.
   */
  implicit lazy val defaultServerProvider: ServerProvider = {
    val classLoader = this.getClass.getClassLoader
    val config = Configuration.load(classLoader, System.getProperties, Map.empty, allowMissingApplicationConf = true)
    fromConfiguration(classLoader, config)
  }

}
