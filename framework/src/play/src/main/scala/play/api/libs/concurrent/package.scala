package play.api.libs

import akka.dispatch.{ Future }

/**
 * Utility classes commonly useful in concurrent programming, such as Promise and Akka helpers.
 * For example:
 * {{{
 *   val promise1 = akka.dispatch.Future{"hello"}.asPromise
 *   val promise2 = Promise.pure(mylongRunningJob)
 * }}}
 */
package object concurrent {

  implicit private[concurrent] val defaultExecutionContext: scala.concurrent.ExecutionContext = scala.concurrent.defaultExecutionContext

  type RedeemablePromise[A] = Promise[A] with Redeemable[A]

  /**
   * Implicit conversion of Future to AkkaFuture, supporting the asPromise operation.
   */
  implicit def akkaToPlay[A](future: Future[A]) = new AkkaFuture(future)

  type Promise[+A] = scala.concurrent.Future[A] 
  implicit def futureToPlayPromise[A](fu:scala.concurrent.Future[A]):PlayPromise[A] = new PlayPromise[A](fu)

  implicit def promiseToRedeemable[A](p:scala.concurrent.Promise[A]):PlayRedeemable[A] = new PlayRedeemable(p)

}
