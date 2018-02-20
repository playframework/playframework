/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import akka.stream.Materializer
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import scala.concurrent.{ Promise, Future }

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
    implicit val ec = mat.executionContext
    new EssentialAction {
      def apply(rh: RequestHeader): Accumulator[ByteString, Result] = {

        // Promised result returned to this filter when it invokes the delegate function (the next filter in the chain)
        val promisedResult = Promise[Result]()
        // Promised accumulator returned to the framework
        val bodyAccumulator = Promise[Accumulator[ByteString, Result]]()

        // Invoke the filter
        val result = self.apply({ (rh: RequestHeader) =>
          // Invoke the delegate
          bodyAccumulator.success(next(rh))
          promisedResult.future
        })(rh)

        result.onComplete({ resultTry =>
          // It is possible that the delegate function (the next filter in the chain) was never invoked by this Filter.
          // Therefore, as a fallback, we try to redeem the bodyAccumulator Promise here with an iteratee that consumes
          // the request body.
          bodyAccumulator.tryComplete(resultTry.map(simpleResult => Accumulator.done(simpleResult)))
        })

        Accumulator.flatten(bodyAccumulator.future.map { it =>
          it.mapFuture { simpleResult =>
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
        })
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

