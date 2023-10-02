/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// Using an `org.apache.pekko` package to make it possible to extend Materializer
// which has some `private[pekko]` methods.
package org.apache.pekko.stream.testkit

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContextExecutor

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Cancellable
import org.apache.pekko.actor.Props
import org.apache.pekko.stream.ActorMaterializerSettings
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.ClosedShape
import org.apache.pekko.stream.Graph
import org.apache.pekko.stream.MaterializationContext
import org.apache.pekko.stream.Materializer

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

  private[pekko] override def logger = throw new UnsupportedOperationException("NoMaterializer does not provide logger")

  private[pekko] override def supervisor =
    throw new UnsupportedOperationException("NoMaterializer does not provide supervisor")

  private[pekko] override def actorOf(context: MaterializationContext, props: Props) =
    throw new UnsupportedOperationException("NoMaterializer does not provide actorOf")

  override def settings: ActorMaterializerSettings =
    throw new UnsupportedOperationException("NoMaterializer does not provide settings")
}
