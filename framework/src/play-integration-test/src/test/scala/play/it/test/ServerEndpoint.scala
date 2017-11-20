/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import play.api.test.PlayRunners
import play.api.{ Application, Mode }
import play.core.server.ServerConfig

import scala.util.control.NonFatal

/**
 * Contains information about the port and protocol used to connect to the server.
 * This class is used to abstract out the details of connecting to different backends
 * and protocols. Most tests will operate the same no matter which endpoint they
 * are connected to.
 */
final case class ServerEndpoint(recipe: ServerEndpointRecipe, port: Int) {
  final override def toString = recipe.description

  /**
   * Create a full URL out of a path. E.g. a path of `/foo` becomes `http://localhost:12345/foo`
   */
  final def pathUrl(path: String): String = s"${recipe.scheme}://localhost:$port$path"
}

object ServerEndpoint {

  /**
   * Starts a server by following a [[ServerEndpointRecipe]] and using the
   * application provided by an [[ApplicationFactory]]. The server's endpoint
   * is passed to the given `block` of code.
   */
  def startEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory): (ServerEndpoint, AutoCloseable) = {
    val application: Application = appFactory.create()

    // Create a ServerConfig with dynamic ports and using a self-signed certificate
    val serverConfig = {
      val sc: ServerConfig = ServerConfig(
        port = endpointRecipe.configuredHttpPort,
        sslPort = endpointRecipe.configuredHttpsPort,
        mode = Mode.Test,
        rootDir = application.path
      )
      val patch = endpointRecipe.serverConfiguration
      sc.copy(configuration = sc.configuration ++ patch)
    }

    // Initialize and start the TestServer
    val testServer: play.api.test.TestServer = new play.api.test.TestServer(
      serverConfig, application, Some(endpointRecipe.serverProvider)
    )

    val runSynchronized = application.globalApplicationEnabled
    if (runSynchronized) {
      PlayRunners.mutex.lock()
    }
    val stopEndpoint = new AutoCloseable {
      override def close(): Unit = {
        testServer.stop()
        if (runSynchronized) {
          PlayRunners.mutex.unlock()
        }
      }
    }
    try {
      testServer.start()
      val endpoint: ServerEndpoint = {
        val port: Int = if (endpointRecipe.configuredHttpPort.isDefined) {
          testServer.runningHttpPort.get
        } else if (endpointRecipe.configuredHttpsPort.isDefined) {
          testServer.runningHttpsPort.get
        } else {
          throw new IllegalStateException("The ServerEndpointRecipe had no configured port")
        }
        ServerEndpoint(endpointRecipe, port)
      }
      (endpoint, stopEndpoint)
    } catch {
      case NonFatal(e) =>
        stopEndpoint.close()
        throw e
    }
  }

  /**
   * Starts an endpoint, runs a block of code, then stops the endpoint.
   */
  def withEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory)(block: ServerEndpoint => A): A = {
    val (endpoint, endpointCloseable) = startEndpoint(endpointRecipe, appFactory)
    try block(endpoint) finally endpointCloseable.close()
  }
}