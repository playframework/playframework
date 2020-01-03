/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// Using an `akka` package to make it possible to extend Materializer
// which has some `private[akka]` methods.
package akka.stream.testkit

import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.actor.Props
import akka.stream.ActorMaterializerSettings
import akka.stream.Attributes
import akka.stream.ClosedShape
import akka.stream.Graph
import akka.stream.MaterializationContext
import akka.stream.Materializer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration

object NoMaterializer extends Materializer {
  override def withNamePrefix(name: String): Materializer =
    throw new UnsupportedOperationException("NoMaterializer does not provide withNamePrefix")

  override def materialize[Mat](runnable: Graph[ClosedShape, Mat]): Mat =
    throw new UnsupportedOperationException("NoMaterializer does not provide materialize")

  override def materialize[Mat](runnable: Graph[ClosedShape, Mat], defaultAttributes: Attributes): Mat =
    throw new UnsupportedOperationException("NoMaterializer does not provide materialize")

  implicit override def executionContext: ExecutionContextExecutor =
    throw new UnsupportedOperationException("NoMaterializer does not provide executionContext")

  override def scheduleOnce(delay: FiniteDuration, task: Runnable): Cancellable =
    throw new UnsupportedOperationException("NoMaterializer does not provide scheduleOnce")

  override def scheduleWithFixedDelay(
      initialDelay: FiniteDuration,
      delay: FiniteDuration,
      task: Runnable
  ): Cancellable = throw new UnsupportedOperationException("NoMaterializer does not provide scheduleWithFixedDelay")

  override def scheduleAtFixedRate(
      initialDelay: FiniteDuration,
      interval: FiniteDuration,
      task: Runnable
  ): Cancellable = throw new UnsupportedOperationException("NoMaterializer does not provide scheduleAtFixedRate")

  override def schedulePeriodically(
      initialDelay: FiniteDuration,
      interval: FiniteDuration,
      task: Runnable
  ): Cancellable = throw new UnsupportedOperationException("NoMaterializer does not provide schedulePeriodically")

  override def shutdown(): Unit = throw new UnsupportedOperationException("NoMaterializer does not provide shutdown")

  override def isShutdown: Boolean =
    throw new UnsupportedOperationException("NoMaterializer does not provide isShutdown")

  override def system: ActorSystem = throw new UnsupportedOperationException("NoMaterializer does not provide system")

  private[akka] override def logger = throw new UnsupportedOperationException("NoMaterializer does not provide logger")

  private[akka] override def supervisor =
    throw new UnsupportedOperationException("NoMaterializer does not provide supervisor")

  private[akka] override def actorOf(context: MaterializationContext, props: Props) =
    throw new UnsupportedOperationException("NoMaterializer does not provide actorOf")

  override def settings: ActorMaterializerSettings =
    throw new UnsupportedOperationException("NoMaterializer does not provide settings")
}
