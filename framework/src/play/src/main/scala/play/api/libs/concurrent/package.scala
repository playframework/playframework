package play.api.libs

import scala.concurrent.{ Future }

/**
 * Utility classes commonly useful in concurrent programming, such as Promise and Akka helpers.
 * For example:
 * {{{
 *   val promise1 = akka.dispatch.Future{"hello"}.asPromise
 *   val promise2 = Promise.pure(mylongRunningJob)
 * }}}
 */
package object concurrent {


  implicit def futureToPlayPromise[A](fu: scala.concurrent.Future[A]): PlayPromise[A] = new PlayPromise[A](fu)

  implicit def promiseToRedeemable[A](p: scala.concurrent.Promise[A]): PlayRedeemable[A] = new PlayRedeemable(p)

}

package concurrent {
  package object backwardCompatible {

  type RedeemablePromise[A] = Future[A] with Redeemable[A]

  }
}
