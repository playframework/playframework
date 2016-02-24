/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import akka.stream.ActorMaterializer
import play.api.http.{ NotImplementedHttpRequestHandler, DefaultHttpErrorHandler }
import play.api.libs.concurrent.ActorSystemProvider
import java.io.File

/**
 * Fake application as used by Play core tests.  This is needed since Play core can't depend on the Play test API.
 * It's also a lot simpler, doesn't load default config files etc.
 */
private[play] case class PlayCoreTestApplication(config: Map[String, Any] = Map(),
    path: File = new File("."),
    mode: Mode.Mode = Mode.Test) extends Application {
  val classloader = Thread.currentThread.getContextClassLoader
  lazy val configuration = Configuration.from(config)
  private val lazyActorSystem = ActorSystemProvider.lazyStart(classloader, configuration)
  def actorSystem = lazyActorSystem.get()
  lazy val materializer = ActorMaterializer()(actorSystem)
  def stop() = lazyActorSystem.close()
  val errorHandler = DefaultHttpErrorHandler
  val requestHandler = NotImplementedHttpRequestHandler
}
