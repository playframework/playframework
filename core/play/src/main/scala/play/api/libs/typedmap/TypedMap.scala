/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.typedmap

import scala.collection.immutable

/**
 * A TypedMap is an immutable map containing typed values. Each entry is
 * associated with a [[TypedKey]] that can be used to look up the value. A
 * `TypedKey` also defines the type of the value, e.g. a `TypedKey[String]`
 * would be associated with a `String` value.
 *
 * Instances of this class are created with the `TypedMap.empty` method.
 *
 * The elements inside TypedMaps cannot be enumerated. This is a decision
 * designed to enforce modularity. It's not possible to accidentally or
 * intentionally access a value in a TypedMap without holding the
 * corresponding [[TypedKey]].
 */
trait TypedMap {

  /**
   * Get a value from the map, throwing an exception if it is not present.
   *
   * @param key The key for the value to retrieve.
   * @tparam A The type of value to retrieve.
   * @return The value, if it is present in the map.
   * @throws scala.NoSuchElementException If the value isn't present in the map.
   */
  def apply[A](key: TypedKey[A]): A

  /**
   * Get a value from the map, returning `None` if it is not present.
   *
   * @param key The key for the value to retrieve.
   * @tparam A The type of value to retrieve.
   * @return `Some` value, if it is present in the map, otherwise `None`.
   */
  def get[A](key: TypedKey[A]): Option[A]

  /**
   * Check if the map contains a value with the given key.
   *
   * @param key The key to check for.
   * @return True if the value is present, false otherwise.
   */
  def contains(key: TypedKey[?]): Boolean

  /**
   * Update the map with the given key and value, returning a new instance of the map.
   *
   * @param key The key to set.
   * @param value The value to use.
   * @tparam A The type of value.
   * @return A new instance of the map with the new entry added.
   */
  def updated[A](key: TypedKey[A], value: A): TypedMap

  /**
   * Update the map with one entry, returning a new instance of the map.
   *
   * @param e1 The new entry to add to the map.
   * @return A new instance of the map with the new entry added.
   */
  def updated(e1: TypedEntry[?]): TypedMap

  /**
   * Update the map with two entries, returning a new instance of the map.
   *
   * @param e1 The first new entry to add to the map.
   * @param e2 The second new entry to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  def updated(e1: TypedEntry[?], e2: TypedEntry[?]): TypedMap

  /**
   * Update the map with three entries, returning a new instance of the map.
   *
   * @param e1 The first new entry to add to the map.
   * @param e2 The second new entry to add to the map.
   * @param e3 The third new entry to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  def updated(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): TypedMap

  /**
   * Update the map with several entries, returning a new instance of the map.
   *
   * @param entries The new entries to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  def updated(entries: TypedEntry[?]*): TypedMap

  /**
   * Update the map with several entries, returning a new instance of the map.
   *
   * @param entries The new entries to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  @deprecated(message = "Use `updated` instead.", since = "2.9.0")
  def +(entries: TypedEntry[?]*): TypedMap

  /**
   * Removes a key from the map, returning a new instance of the map.
   *
   * @param e1 The key to remove.
   * @return A new instance of the map with the entry removed.
   */
  def removed(e1: TypedKey[?]): TypedMap

  /**
   * Removes two keys from the map, returning a new instance of the map.
   *
   * @param e1 The first key to remove.
   * @param e2 The second key to remove.
   * @return A new instance of the map with the entries removed.
   */
  def removed(e1: TypedKey[?], e2: TypedKey[?]): TypedMap

  /**
   * Removes three keys from the map, returning a new instance of the map.
   *
   * @param e1 The first key to remove.
   * @param e2 The second key to remove.
   * @param e3 The third key to remove.
   * @return A new instance of the map with the entries removed.
   */
  def removed(e1: TypedKey[?], e2: TypedKey[?], e3: TypedKey[?]): TypedMap

  /**
   * Removes keys from the map, returning a new instance of the map.
   *
   * @param keys The keys to remove.
   * @return A new instance of the map with the entries removed.
   */
  def removed(keys: TypedKey[?]*): TypedMap

  /**
   * Removes keys from the map, returning a new instance of the map.
   *
   * @param keys The keys to remove.
   * @return A new instance of the map with the entries removed.
   */
  @deprecated(message = "Use `removed` instead.", since = "2.9.0")
  def -(keys: TypedKey[?]*): TypedMap

  /**
   * @return The Java version for this map.
   */
  def asJava: play.libs.typedmap.TypedMap = new play.libs.typedmap.TypedMap(this)
}

object TypedMap {

  /**
   * The empty [[TypedMap]] instance.
   */
  val empty = new DefaultTypedMap(immutable.Map.empty)

  /**
   * Builds a [[TypedMap]] from an entry of key and value.
   */
  def apply(e1: TypedEntry[?]): TypedMap = TypedMap.empty + e1

  /**
   * Builds a [[TypedMap]] from two entries of keys and values.
   */
  def apply(e1: TypedEntry[?], e2: TypedEntry[?]): TypedMap = TypedMap.empty + (e1, e2)

  /**
   * Builds a [[TypedMap]] from three entries of keys and values.
   */
  def apply(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): TypedMap = TypedMap.empty + (e1, e2, e3)

  /**
   * Builds a [[TypedMap]] from a list of keys and values.
   */
  def apply(entries: TypedEntry[?]*): TypedMap = {
    TypedMap.empty.+(entries*)
  }
}

/**
 * An implementation of `TypedMap` that wraps a standard Scala [[Map]].
 */
private[typedmap] final class DefaultTypedMap private[typedmap] (m: immutable.Map[TypedKey[?], Any]) extends TypedMap {
  override def apply[A](key: TypedKey[A]): A                           = m.apply(key).asInstanceOf[A]
  override def get[A](key: TypedKey[A]): Option[A]                     = m.get(key).asInstanceOf[Option[A]]
  override def contains(key: TypedKey[?]): Boolean                     = m.contains(key)
  override def updated[A](key: TypedKey[A], value: A): TypedMap        = new DefaultTypedMap(m.updated(key, value))
  override def updated(e1: TypedEntry[?]): TypedMap                    = new DefaultTypedMap(m.updated(e1.key, e1.value))
  override def updated(e1: TypedEntry[?], e2: TypedEntry[?]): TypedMap =
    new DefaultTypedMap(m.updated(e1.key, e1.value).updated(e2.key, e2.value))
  override def updated(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): TypedMap =
    new DefaultTypedMap(m.updated(e1.key, e1.value).updated(e2.key, e2.value).updated(e3.key, e3.value))
  override def updated(entries: TypedEntry[?]*): TypedMap = {
    val m2 = entries.foldLeft(m) {
      case (m1, e) => m1.updated(e.key, e.value)
    }
    new DefaultTypedMap(m2)
  }
  override def +(entries: TypedEntry[?]*): TypedMap                = updated(entries*)
  override def removed(k1: TypedKey[?]): TypedMap                  = new DefaultTypedMap(m - k1)
  override def removed(k1: TypedKey[?], k2: TypedKey[?]): TypedMap =
    new DefaultTypedMap(m - k1 - k2)
  override def removed(k1: TypedKey[?], k2: TypedKey[?], k3: TypedKey[?]): TypedMap =
    new DefaultTypedMap(m - k1 - k2 - k3)
  override def removed(keys: TypedKey[?]*): TypedMap = new DefaultTypedMap(m.removedAll(keys))
  override def -(keys: TypedKey[?]*): TypedMap       = removed(keys*)
  override def toString: String                      = m.mkString("{", ", ", "}")
}
