/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.typedmap

/**
 * An entry that binds a typed key and a value. These entries can be
 * placed into a [[TypedMap]] or any other type of object with typed
 * values.
 *
 * @param key The key for this entry.
 * @param value The value for this entry.
 * @tparam A The type of the value.
 */
final case class TypedEntry[A](key: TypedKey[A], value: A) {
  /**
   * Convert the entry into a standard pair.
   */
  def toPair: (TypedKey[A], A) = (key, value)
}
