/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
 * should implement the `create()` method to create this value. This method returns
 * that value along with a second value representing the resource that must be closed.
 * The resource might be the same as the main value but it can also be different. E.g.
 * the public value might be an ExecutionContext, but the type of value to close might
 * be the ExecutorService that the ExecutionContext warps.
 */
private[play] abstract class ClosableLazy[T <: AnyRef] {

  /**
   * The type of resource to close when `close()` is called. May be different
   * from the type of T.
   */
  protected type ResourceToClose <: AnyRef
  /**
   * The result of calling the `create()` method.
   *
   * @param value The value that has been initialized.
   * @param resourceToClose A resource that has been allocated. This resource will be
   * passed to `close(ResourceToClose)`  when `close()` is called.
   */
  protected case class CreateResult(value: T, resourceToClose: ResourceToClose)

  @volatile
  private var value: AnyRef = null
  private var resourceToClose: AnyRef = null
  private var hasBeenClosed: Boolean = false

  /**
   * Get the value. Calling this method may allocate resources, such as a thread pool.
   *
   * Calling this method after the `close()` method has been called will result in an
   * IllegalStateException.
   */
  final def get(): T = {
    val currentValue = value
    if (currentValue != null) return currentValue.asInstanceOf[T]
    synchronized {
      if (hasBeenClosed) throw new IllegalStateException("Can't get ClosableLazy value after it has been closed")
      if (value == null) {
        val result = create()
        if (result.value == null) throw new IllegalStateException("Can't initialize ClosableLazy to null value")
        value = result.value
        resourceToClose = result.resourceToClose
      }
      value.asInstanceOf[T]
    }
  }

  /**
   * Close the value. Calling this method is safe, but does nothing, if the value
   * has not been initialized.
   */
  final def close(): Unit = {
    synchronized {
      if (!hasBeenClosed && value != null) {
        val cachedCloseInfo = resourceToClose.asInstanceOf[ResourceToClose]
        value = null
        hasBeenClosed = true
        resourceToClose = null
        close(cachedCloseInfo)
      }
    }

  }

  /**
   * Called when the lazy value is first initialized. Returns the value, and the
   * resource to close when `close()` is called.
   */
  protected def create(): CreateResult

  /**
   * Called when `close()` is called. Passed the resource that was originally
   * returned when `create()` was called.
   */
  protected def close(resourceToClose: ResourceToClose)
}