package play.api.mvc

import play.api._
import play.api.libs.iteratee._
import scala.concurrent.{ Promise, Future }

/**
 * Implement this interface if you want to add a Filter to your application
 * {{{
 * object AccessLog extends Filter {
 *   override def apply(next: RequestHeader => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
 * 		 val result = next(request)
 * 		 result.map { r => play.Logger.info(request + "\n\t => " + r; r }
 * 	 }
 * }
 * }}}
 */
trait EssentialFilter {
  def apply(next: EssentialAction): EssentialAction
}

trait Filter extends EssentialFilter {

  self =>

  def apply(f: RequestHeader => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult]

  def apply(next: EssentialAction): EssentialAction = {
    new EssentialAction {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext

      def apply(rh: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {

        // Promised result, that is returned to the filter when it invokes the wrapped function
        val promisedResult = Promise[SimpleResult]
        // Promised iteratee, that we return to the framework
        val bodyIteratee = Promise[Iteratee[Array[Byte], SimpleResult]]

        // Invoke the filter
        val result = self.apply({ (rh: RequestHeader) =>
          // Invoke the delegate
          bodyIteratee.success(next(rh))
          promisedResult.future
        })(rh)

        result.onComplete({ resultTry =>
          // If we've got a result, but the body iteratee isn't redeemed, then that means the delegate action
          // wasn't invoked.  In which case, we need to supply an iteratee to consume the request body.
          if (!bodyIteratee.isCompleted) {
            bodyIteratee.complete(resultTry.map(simpleResult => Done(simpleResult)))
          }
        })

        // When the iteratee is done, we can redeem the result that was returned to the filter
        Iteratee.flatten(bodyIteratee.future.map(_.mapM({ simpleResult =>
          promisedResult.success(simpleResult)
          result
        })))
      }

    }
  }
}

object Filter {
  def apply(filter: (RequestHeader => Future[SimpleResult], RequestHeader) => Future[SimpleResult]): Filter = new Filter {
    def apply(f: RequestHeader => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = filter(f, rh)
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
    def apply(rh: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {
      val chain = filters.reverse.foldLeft(action) { (a, i) => i(a) }
      chain(rh)
    }
  }
}

