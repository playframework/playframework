/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.prop

/**
 * A property is a key that can be used to get and set values on an
 * object that implements [[HasProps]].
 *
 * @tparam A The type of values associated with this property.
 */
trait Prop[A] {

  /**
   * Bind this property to a value.
   *
   * @param value The value to bind this property to.
   * @return A bound value.
   */
  def withValue(value: A): Prop.WithValue[A] = Prop.WithValue(this, value)

  /**
   * Bind this property to a value.
   * @param value
   * @return
   */
  def ~>(value: A): Prop.WithValue[A] = withValue(value)
}

/**
 * Helper for working with `Prop`s.
 */
object Prop {
  /**
   *
   * @param displayName
   * @tparam A
   * @return
   */
  def apply[A](displayName: String): Prop[A] = new Prop[A] {
    override def toString: String = displayName
  }
  final case class WithValue[A](prop: Prop[A], value: A)
}