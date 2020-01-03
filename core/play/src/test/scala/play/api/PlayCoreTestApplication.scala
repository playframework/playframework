/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io.File

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorHandler
import play.api.http.HttpRequestHandler
import play.api.http.NotImplementedHttpRequestHandler
import play.api.libs.concurrent.ActorSystemProvider
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.RequestFactory

import scala.concurrent.Future

/**
 * Fake application as used by Play core tests.  This is needed since Play core can't depend on the Play test API.
 * It's also a lot simpler, doesn't load default config files etc.
 */
private[play] case class PlayCoreTestApplication(
    config: Map[String, Any] = Map(),
    path: File = new File("."),
    override val mode: Mode = Mode.Test
) extends Application {
  def this() = this(config = Map())

  private var _terminated   = false
  def isTerminated: Boolean = _terminated

  override val classloader: ClassLoader          = Thread.currentThread.getContextClassLoader
  override lazy val environment: Environment     = Environment.simple(path, mode)
  override lazy val configuration: Configuration = Configuration.from(config)

  override lazy val requestFactory: RequestFactory     = new DefaultRequestFactory(httpConfiguration)
  override lazy val errorHandler: HttpErrorHandler     = DefaultHttpErrorHandler
  override lazy val requestHandler: HttpRequestHandler = NotImplementedHttpRequestHandler

  override lazy val actorSystem: ActorSystem                 = ActorSystemProvider.start(classloader, configuration)
  override lazy val materializer: Materializer               = Materializer.matFromSystem(actorSystem)
  override lazy val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  def stop(): Future[Unit] = {
    implicit val ctx = actorSystem.dispatcher
    coordinatedShutdown
      .run(CoordinatedShutdown.UnknownReason)
      .map(_ => _terminated = true)
  }
}
