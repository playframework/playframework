/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.lang.ref.WeakReference

/**
 * Creates a wrapper for a function that uses an inline cache to
 * optimize calls to the function. The function's result for a
 * a *single* input will be cached. If the input changes, the
 * function will be called again and its new output will be cached.
 *
 * Even though this function only caches a single value, it can
 * be useful for functions where the input only changes occasionally.
 * Because it only caches a single value, the cached value can
 * be accessed very quickly.
 *
 * This class will improve performance when the function input changes
 * infrequently and the function is somewhat expensive to call.
 * Even when the input changes, this class will still function
 * correctly and return the correct value, although it will be less
 * efficient than an unwrapped function because it will update
 * the cache.
 *
 * The cached input and output will be wrapped by a WeakReference
 * so that they can be garbage collected. This may mean that the
 * cache needs to be repopulated after garbage collection has
 * been run.
 *
 * The function may sometimes be called again for the same input.
 * In the current implementation this happens in order to avoid
 * synchronizing the cached value across threads. It may also
 * happen when the weakly-referenced cache is cleared by the
 * garbage collector.
 *
 * Reference equality is used to compare inputs, for speed and
 * to be conservative.
 */
private[play] final class InlineCache[A <: AnyRef, B](f: A => B) extends (A => B) {
  /**
   * For performance, don't synchronize this value. Instead, let
   * the cache be updated on different threads. If the input value
   * is stable then the value of this variable will eventually
   * reach the same value. If the input value is different, then
   * there's no point sharing the value across threads anyway.
   */
  var cache: WeakReference[(A, B)] = null

  override def apply(a: A): B = {
    // Get the current value of the cache into a local variable.
    // If it's null then this is our first call to the function
    // (on this thread) so get a fresh value.
    val cacheSnapshot = cache
    if (cacheSnapshot == null) return fresh(a)
    // Get cached input/output pair out of the WeakReference.
    // If the pair is null then the reference has been collected
    // and we need a fresh value.
    val inputOutput = cacheSnapshot.get
    if (inputOutput == null) return fresh(a)
    // If the inputs don't match then we need a fresh value.
    if (inputOutput._1 ne a) return fresh(a)
    // We got the cached value, return it.
    inputOutput._2
  }

  /** Get a fresh value and update the cache with it. */
  private def fresh(a: A): B = {
    val b = f(a)
    cache = new WeakReference((a, b))
    b
  }
}
