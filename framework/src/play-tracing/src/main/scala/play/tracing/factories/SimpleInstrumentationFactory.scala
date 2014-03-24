/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.factories

import play.instrumentation.spi.{ PlayInstrumentation, PlayInstrumentationFactory }
import play.tracing.actors.{ AnalyzerSystem, SimpleBootstrap }
import com.typesafe.config.{ ConfigFactory, Config }
import java.util.concurrent.TimeUnit._
import akka.actor.ActorRef
import akka.pattern.ask
import scala.concurrent.{ Future, Await }
import akka.util.Timeout
import play.tracing.tracers.SimpleTracer

class SimpleInstrumentationFactory extends PlayInstrumentationFactory with SimpleBootstrap {
  val actorSystemName: String = "simple-play-instrumentation"

  val config: Config = ConfigFactory.load().atPath(actorSystemName)

  val analyzerSystemName: String = "simple-play-analyzer-system"

  implicit def askTimeout: Timeout = Timeout(5, SECONDS)

  lazy val collector: ActorRef = {
    val c: Future[AnalyzerSystem.Collector] = (analyzerSystem ask AnalyzerSystem.RequestCollector).mapTo[AnalyzerSystem.Collector]
    Await.result(c, askDuration).ref
  }

  def createPlayInstrumentation(): PlayInstrumentation = {
    new SimpleTracer(srtr => collector ! srtr)
  }
}
