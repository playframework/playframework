/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

/**
 * A cell represents a memory location that stores a value. The cell abstracts
 * away the details of how the value is stored. For example, an [[AssignedCell]]
 * stores a simple value, whereas a [[LazyCell]] only calculates the value when
 * it is first needed.
 */
trait Cell[+A] {
  /**
   * The value in the cell. Calling this method may force the value to be evaluated.
   */
  def value: A

  /**
   * Whether or not the cell value has been evaluated yet. Sometimes it is useful
   * to know this to avoid unnecessarily evaluating the cell value.
   */
  def evaluated: Boolean

  override def toString: String = if (evaluated) s"Container<$value>" else "Container<?>"
}

object Cell {
  /**
   * Create a cell with the given value assigned to it.
   */
  def apply[A](value: A): Cell[A] = new AssignedCell[A](value)
}

/**
 * A cell with a fixed value assigned to it.
 */
final class AssignedCell[+A](override val value: A) extends Cell[A] {
  def evaluated = true
}

/**
 * A cell that evaluates its value on demand. Cell access is unsychronized
 * for performance reasons. However the cell may be safely accessed from multiple threads
 * provided its `create` method is idempotent.
 */
abstract class LazyCell[A] extends Cell[A] {
  private var createdValue: A = emptyMarker

  /**
   * A value of type `A` that indicates the cell hasn't been evaluated. Common
   * values are `null`, `None` or `0`. It's important the marker is not a valid
   * value.
   */
  protected def emptyMarker: A

  /**
   * Create a value. This method is called when the lazy cell is first accessed.
   * After it is first accessed the value will be cached. There is a chance this
   * method will be called more than once if the cell is accessed from multiple
   * threads.
   */
  protected def create: A

  override def value: A = {
    if (createdValue == emptyMarker) {
      createdValue = create
    }
    createdValue
  }

  override def evaluated: Boolean = createdValue != emptyMarker
}
