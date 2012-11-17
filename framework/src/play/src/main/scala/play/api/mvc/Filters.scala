package play.api.mvc

import play.api._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

/**
 * Implement this interface if you want to add a Filter to your application
 * {{{
 * object AccessLog extends Filter {
 * 	 override def apply[A](next: Request[A] => Result)(request: Request[A]): Result = {
 *		 val result = next(request)
 *		 play.Logger.info(request + "\n\t => " + result)
 *		 result
 *	 }
 * }
 * }}}
 */
trait EssentialFilter {
  def apply(next: EssentialAction): EssentialAction
}

trait Filter extends EssentialFilter {

  self =>

  def apply(f:RequestHeader => Result)(rh:RequestHeader):Result

  def apply(next: EssentialAction): EssentialAction = {
    val p = scala.concurrent.Promise[Result]()

    new EssentialAction {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      def apply(rh:RequestHeader):Iteratee[Array[Byte],Result] = {
        val it = scala.concurrent.Promise[Iteratee[Array[Byte],Result]]()
        val result = self.apply({(rh:RequestHeader) => it.success(next(rh)) ; AsyncResult(p.future)})(rh)
        val i = it.future.map(_.map({r => p.success(r); result}))
        result match {
          case r:AsyncResult => Iteratee.flatten( r.unflatten.map(Done(_,Input.Empty: Input[Array[Byte]])).or(i).map(_.fold(identity, identity)))
          case r:PlainResult => Done(r)
        }
      }

    }
  }
}

object Filter {
  def apply(filter:(RequestHeader => Result, RequestHeader) => Result): Filter = new Filter {
    def apply(f:RequestHeader => Result)(rh:RequestHeader):Result = filter(f,rh)
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
  override def doFilter(a:EssentialAction): EssentialAction = {
    Filters(super.doFilter(a),filters:_*)
  }
}

/**
 * Compose the action and the Filters to create a new Action
 */

object FilterChain{
  def apply[A](action:EssentialAction, filters: List[EssentialFilter]): EssentialAction = new EssentialAction {
    def apply(rh:RequestHeader):Iteratee[Array[Byte],Result] = {
      val chain = filters.reverse.foldLeft(action){ (a, i) => i(a) }
      chain(rh)
    }
  }
}

