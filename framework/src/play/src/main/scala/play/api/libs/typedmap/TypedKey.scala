/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.typedmap

/**
 * A TypedKey is a key that can be used to get and set values in a
 * [[TypedMap]] or any object with typed keys. This class uses reference
 * equality for comparisons, so each new instance is different key.
 *
 * @param displayName The name to display for this key or `null` if
 * no display name has been provided. This name is only used for debugging.
 * Keys with the same name are not considered to be equal.
 * @tparam A The type of values associated with this key.
 */
final class TypedKey[A] private (val displayName: Option[String]) {

  /**
   * Bind this key to a value. This is equivalent to the `->` operator.
   *
   * @param value The value to bind this key to.
   * @return A bound value.
   */
  def bindValue(value: A): TypedEntry[A] = TypedEntry(this, value)

  /**
   * Bind this key to a value. Equivalent to [[bindValue]].
   *
   * @param value The value to bind.
   * @return An entry binding this key to a value of the right type.
   */
  def ->(value: A): TypedEntry[A] = bindValue(value)

  override def toString: String = displayName.getOrElse(super.toString)

  /**
   * @return The Java version for this key.
   */
  def asJava: play.libs.typedmap.TypedKey[A] = new play.libs.typedmap.TypedKey[A](this)
}

/**
 * Helper for working with `TypedKey`s.
 */
object TypedKey {

  /**
   * Creates a [[TypedKey]] without a name.
   *
   * @tparam A The type of value this key is associated with.
   * @return A fresh key.
   */
  def apply[A]: TypedKey[A] = new TypedKey[A](None)

  /**
   * Creates a [[TypedKey]] with the given name.
   *
   * @param displayName The name to display when printing this key.
   * @tparam A The type of value this key is associated with.
   * @return A fresh key.
   */
  def apply[A](displayName: String): TypedKey[A] = new TypedKey[A](Some(displayName))
}