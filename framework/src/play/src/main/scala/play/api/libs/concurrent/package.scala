package play.api.libs

/**
 * Utility classes commonly useful in concurrent programming, such as Promise.
 */
package object concurrent {

  type RedeemablePromise[A] = Promise[A] with Redeemable[A]

}