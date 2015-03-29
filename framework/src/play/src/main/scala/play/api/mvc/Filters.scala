/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api._
import play.api.libs.iteratee._
import scala.concurrent.{ Promise, Future }

trait EssentialFilter {
  def apply(next: EssentialAction): EssentialAction
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

  def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result]

  def apply(next: EssentialAction): EssentialAction = {
    new EssentialAction {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext

      def apply(rh: RequestHeader): Iteratee[Array[Byte], Result] = {

        // Promised result returned to this filter when it invokes the delegate function (the next filter in the chain)
        val promisedResult = Promise[Result]()
        // Promised iteratee returned to the framework
        val bodyIteratee = Promise[Iteratee[Array[Byte], Result]]()

        // Invoke the filter
        val result = self.apply({ (rh: RequestHeader) =>
          // Invoke the delegate
          bodyIteratee.success(next(rh))
          promisedResult.future
        })(rh)

        result.onComplete({ resultTry =>
          // It is possible that the delegate function (the next filter in the chain) was never invoked by this Filter. 
          // Therefore, as a fallback, we try to redeem the bodyIteratee Promise here with an iteratee that consumes 
          // the request body.
          bodyIteratee.tryComplete(resultTry.map(simpleResult => Done(simpleResult)))
        })

        Iteratee.flatten(bodyIteratee.future.map { it =>
          it.mapM { simpleResult =>
            // When the iteratee is done, we can redeem the promised result that was returned to the filter
            promisedResult.success(simpleResult)
            result
          }.recoverM {
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
  def apply(filter: (RequestHeader => Future[Result], RequestHeader) => Future[Result]): Filter = new Filter {
    def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = filter(f, rh)
  }
}

/**
 * Compose the action and the Filters to create a new Action
 */
object Filters {
  def apply(h: EssentialAction, filters: EssentialFilter*) = h match {
    case a: EssentialAction => FilterChain(a, filters.toList)
    case h => h
  }
}

class WithFilters(filters: EssentialFilter*) extends GlobalSettings {
  override def doFilter(a: EssentialAction): EssentialAction = {
    Filters(super.doFilter(a), filters: _*)
  }
}

/**
 * Compose the action and the Filters to create a new Action
 */

object FilterChain {
  def apply[A](action: EssentialAction, filters: List[EssentialFilter]): EssentialAction = new EssentialAction {
    def apply(rh: RequestHeader): Iteratee[Array[Byte], Result] = {
      val chain = filters.reverse.foldLeft(action) { (a, i) => i(a) }
      chain(rh)
    }
  }
}

