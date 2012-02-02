package play.api.libs

import akka.dispatch.{ Future }

/**
 * Utility classes commonly useful in concurrent programming, such as Promise and Akka helpers.
 */
package object concurrent {

  type RedeemablePromise[A] = Promise[A] with Redeemable[A]

  /**
   * Implicit conversion of Future to AkkaFuture, supporting the asPromise operation.
   */
  implicit def akkaToPlay[A](future: Future[A]) = new AkkaFuture(future)

}