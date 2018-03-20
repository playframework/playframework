/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.libs.streams.Accumulator

import scala.concurrent.{ ExecutionContext, Future, Promise }

trait EssentialFilter {
  def apply(next: EssentialAction): EssentialAction

  def asJava: play.mvc.EssentialFilter = new play.mvc.EssentialFilter {
    override def apply(next: play.mvc.EssentialAction) = EssentialFilter.this(next).asJava
  }
}

/**
 * Implement this interface if you want to add a Filter to your application
 * {{{
 * object AccessLog extends Filter {
 *   override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
 * 		 val result = next(request)
 * 		 result.map { r => play.Logger.info(request + "\n\t => " + r; r }
 * 	 }
 * }
 * }}}
 */
trait Filter extends EssentialFilter {
  self =>

  implicit def mat: Materializer

  /**
   * Apply the filter, given the request header and a function to call the next
   * operation.
   *
   * @param f A function to call the next operation. Call this to continue
   * normally with the current request. You do not need to call this function
   * if you want to generate a result in a different way.
   * @param rh The RequestHeader.
   */
  def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result]

  def apply(next: EssentialAction): EssentialAction = {
    import play.core.Execution.Implicits.trampoline
    new EssentialAction {
      def apply(rh: RequestHeader): Accumulator[ByteString, Result] = {
        val promisedResult = Promise[Result]()
        // uses a stateful variable to avoid unnecessary Materialization
        var bodyAccumulator: Option[Accumulator[ByteString, Result]] = None

        val result = self.apply({ (rh: RequestHeader) =>
          bodyAccumulator = Some(next(rh))
          promisedResult.future
        })(rh)

        // if no Accumulator was set, we know that the next function was never called
        // so we can just ignore the body
        bodyAccumulator.getOrElse(Accumulator.done(result)).mapFuture{ simpleResult =>
          // When the iteratee is done, we can redeem the promised result that was returned to the filter
          promisedResult.success(simpleResult)
          result
        }.recoverWith {
          case t: Throwable =>
            // If the iteratee finishes with an error, fail the promised result that was returned to the
            // filter with the same error. Note, we MUST use tryFailure here as it's possible that a)
            // promisedResult was already completed successfully in the mapM method above but b) calculating
            // the result in that method caused an error, so we ended up in this recover block anyway.
            promisedResult.tryFailure(t)
            result
        }
      }

    }
  }
}

object Filter {
  def apply(filter: (RequestHeader => Future[Result], RequestHeader) => Future[Result])(implicit m: Materializer): Filter = new Filter {
    implicit def mat = m
    def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = filter(f, rh)
  }
}

/**
 * Compose the action and the Filters to create a new Action
 */
object Filters {
  def apply(h: EssentialAction, filters: EssentialFilter*): EssentialAction = FilterChain(h, filters.toList)
}

/**
 * Compose the action and the Filters to create a new Action
 */

object FilterChain {
  def apply[A](action: EssentialAction, filters: List[EssentialFilter]): EssentialAction = new EssentialAction {
    def apply(rh: RequestHeader): Accumulator[ByteString, Result] = {
      val chain = filters.reverse.foldLeft(action) { (a, i) => i(a) }
      chain(rh)
    }
  }
}

