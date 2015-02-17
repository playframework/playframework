/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

/**
 * Provides functionality like Scala's builtin lazy values, except allows cleanup of
 * the lazily initialized value. The value is lazily initialized when the `get()` method
 * is first called. Later, if the value is no longer needed, it can be cleaned up by
 * calling the `close()` method.
 *
 * Calling `close()` on an uninitialized ClosableLazy will not initialize the value
 * if it is not initialized. A ClosableLazy can be closed multiple times.
 *
 * After being closed, the value cannot be initialized again. ClosableLazy is designed
 * to make it easier to clean up resources when shutting down Play. If resources were able
 * to be reinitialized after closing, then it would be easy to accidentally allocate resources
 * when shutting down. To prevert reinitialization, galling the `get()` method after `close()`
 * will result in an `IllegalStateException`.
 *
 * The performance of this class should be similar to Scala's lazy values. Once initialized,
 * a single read of a volatile value is all that is needed to get the value. If the value is not initialized,
 * then initialization occurs, Initialization is synchronized on the ClosableLazy object.
 *
 * This class exposes `T` as the type of value that is lazily created. Subclasses
 * should implement the `create()` method to create this value. The `create()` method
 * also returns a function that will be called when `close()` is called. This allows
 * any resources associated with the value to be closed.
 */
private[play] abstract class ClosableLazy[T >: Null <: AnyRef] {

  protected type CloseFunction = (() => Unit)

  @volatile
  private var value: T = null
  private var closeFunction: CloseFunction = null
  private var hasBeenClosed: Boolean = false

  /**
   * Get the value. Calling this method may allocate resources, such as a thread pool.
   *
   * Calling this method after the `close()` method has been called will result in an
   * IllegalStateException.
   */
  final def get(): T = {
    val currentValue = value
    if (currentValue != null) return currentValue
    synchronized {
      if (hasBeenClosed) throw new IllegalStateException("Can't get ClosableLazy value after it has been closed")
      if (value == null) {
        val (v, cf): (T, CloseFunction) = create()
        if (v == null) throw new IllegalStateException("Can't initialize ClosableLazy to a null value")
        if (cf == null) throw new IllegalStateException("Can't initialize ClosableLazy's close function to a null value")
        value = v
        closeFunction = cf
        v
      } else {
        // Value was initialized by another thread before we got the monitor
        value
      }
    }
  }

  /**
   * Close the value. Calling this method is safe, but does nothing, if the value
   * has not been initialized.
   */
  final def close(): Unit = {
    val optionalClose: Option[CloseFunction] = synchronized {
      if (hasBeenClosed) {
        // Already closed
        None
      } else if (value == null) {
        // Close before first call to get
        hasBeenClosed = true
        None
      } else {
        // Close and call the close function
        hasBeenClosed = true
        val prevCloseFunction = closeFunction
        value = null
        closeFunction = null
        Some(prevCloseFunction)
      }
    }
    // Perform actual close outside the synchronized block,
    // just in case the close function calls get or close
    // from another thread.
    optionalClose.foreach(_.apply())
  }

  /**
   * Called when the lazy value is first initialized. Returns the value and
   * a function to close the value when `close` is called.
   */
  protected def create(): (T, CloseFunction)
}
