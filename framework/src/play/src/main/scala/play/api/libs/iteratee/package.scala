package play.api.libs

/**
 * The Iteratee monad provides strict, safe, and functional I/O.
 */
package object iteratee {

  type K[E, A] = Input[E] => Iteratee[E, A]

  implicit private[iteratee] val defaultExecutionContext: scala.concurrent.ExecutionContext = scala.concurrent.defaultExecutionContext

}
