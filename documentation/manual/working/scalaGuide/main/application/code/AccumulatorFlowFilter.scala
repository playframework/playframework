/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.advanced.filters.essential
import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.mvc._
import play.api.libs.streams._

import scala.concurrent.ExecutionContext

/**
 * Demonstrates the use of an accumulator with flow.
 */
// #essential-filter-flow-example
class AccumulatorFlowFilter @Inject()(actorSystem: ActorSystem)(implicit ec: ExecutionContext) extends EssentialFilter {

  private val logger = org.slf4j.LoggerFactory.getLogger("application.AccumulatorFlowFilter")

  private implicit val logging = Logging(actorSystem.eventStream, logger.getName)

  override def apply(next: EssentialAction): EssentialAction = new EssentialAction {
    override def apply(request: RequestHeader): Accumulator[ByteString, Result] = {
      val accumulator: Accumulator[ByteString, Result] = next(request)

      val flow: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].log("byteflow")
      val accumulatorWithResult = accumulator.through(flow).map { result =>
        logger.info(s"The flow has completed and the result is $result")
        result
      }

      accumulatorWithResult
    }
  }
}
// #essential-filter-flow-example
