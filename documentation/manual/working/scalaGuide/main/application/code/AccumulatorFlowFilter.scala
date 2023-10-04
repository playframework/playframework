/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.filters.essential
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.Logging
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.apache.pekko.NotUsed
import play.api.libs.streams._
import play.api.mvc._

/**
 * Demonstrates the use of an accumulator with flow.
 */
// #essential-filter-flow-example
class AccumulatorFlowFilter @Inject() (actorSystem: ActorSystem)(implicit ec: ExecutionContext)
    extends EssentialFilter {
  private val logger = org.slf4j.LoggerFactory.getLogger("application.AccumulatorFlowFilter")

  private implicit val logging: LoggingAdapter = Logging(actorSystem.eventStream, logger.getName)

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
