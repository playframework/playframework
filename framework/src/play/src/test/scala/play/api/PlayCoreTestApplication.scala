/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import akka.stream.ActorMaterializer
import play.api.http.{ DefaultHttpErrorHandler, NotImplementedHttpRequestHandler }
import play.api.libs.concurrent.ActorSystemProvider
import java.io.File

import play.api.mvc.request.DefaultRequestFactory

/**
 * Fake application as used by Play core tests.  This is needed since Play core can't depend on the Play test API.
 * It's also a lot simpler, doesn't load default config files etc.
 */
private[play] case class PlayCoreTestApplication(
    config: Map[String, Any] = Map(),
    path: File = new File("."),
    override val mode: Mode = Mode.Test) extends Application {

  def this() = this(config = Map())

  private var _terminated = false
  def isTerminated: Boolean = _terminated

  val classloader = Thread.currentThread.getContextClassLoader
  lazy val configuration = Configuration.from(config)
  private val lazyActorSystem = ActorSystemProvider.lazyStart(classloader, configuration)
  def actorSystem = lazyActorSystem.get()
  lazy val materializer = ActorMaterializer()(actorSystem)
  lazy val requestFactory = new DefaultRequestFactory(httpConfiguration)
  val errorHandler = DefaultHttpErrorHandler
  val requestHandler = NotImplementedHttpRequestHandler
  override lazy val environment: Environment = Environment.simple(path, mode)

  def stop() = {
    _terminated = true
    lazyActorSystem.close()
  }
}
