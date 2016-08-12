/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.typedmap

import scala.annotation.varargs
import scala.collection.immutable

/**
 * A TypedMap is an immutable map containing typed values. Each entry is
 * associated with a [[TypedKey]] that can be used to look up the value. A
 * `TypedKey` also defines the type of the value, e.g. a `TypedKey[String]`
 * would be associated with a `String` value.
 *
 * Instances of this class are created with the `TypedMap.empty` method.
 *
 * @param m The map used to store values.
 */
final class TypedMap private (m: immutable.Map[TypedKey[_], Any]) {
  /**
   * Get a value from the map.
   *
   * @param key The key for the value to retrieve.
   * @tparam A The type of value to retrieve.
   * @return The value, if it is present in the map.
   * @throws NoSuchElementException If the value isn't present in the map.
   */
  def apply[A](key: TypedKey[A]): A = m.apply(key).asInstanceOf[A]

  /**
   * Get a value from the map.
   *
   * @param key The key for the value to retrieve.
   * @tparam A The type of value to retrieve.
   * @return `Some` value, if it is present in the map, otherwise `None`.
   */
  def get[A](key: TypedKey[A]): Option[A] = m.get(key).asInstanceOf[Option[A]]

  /**
   * Check if the map contains a value with the given key.
   *
   * @param key The key to check for.
   * @return True if the value is present, false otherwise.
   */
  def contains(key: TypedKey[_]): Boolean = m.contains(key)

  /**
   * Update the map with the given key and value, returning a new instance of the map.
   *
   * @param key The key to set.
   * @param value The value to use.
   * @tparam A The type of value.
   * @return A new instance of the map with the new entry added.
   */
  def updated[A](key: TypedKey[A], value: A): TypedMap = new TypedMap(m.updated(key, value))

  /**
   * Update the map with several entries, returning a new instance of the map.
   *
   * @param entries The new entries to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  def +(entries: TypedEntry[_]*): TypedMap = {
    val m2 = entries.foldLeft(m) {
      case (m1, e) => m1.updated(e.key, e.value)
    }
    new TypedMap(m2)
  }

  /**
   * Update the map with several entries, returning a new instance of the map.
   *
   * This is a variant of the `+`, provided for use from Java.
   *
   * @param entries The new entries to add to the map.
   * @return A new instance of the map with the new entries added.
   */
  @varargs
  def withEntries(entries: TypedEntry[_]*): TypedMap = this + (entries: _*)

  /**
   * Gets the entries present in this map.
   */
  def entries: immutable.Seq[TypedEntry[_]] = {
    // Use local function to capture type of key
    def makeEntry[A](key: TypedKey[A], value: Any): TypedEntry[_] = {
      key.bindValue(value.asInstanceOf[A])
    }
    m.map { case (key, value) => makeEntry(key, value) }.to[immutable.Seq]
  }

  override def toString: String = m.mkString
}

object TypedMap {

  /**
   * The empty [[TypedMap]] instance.
   */
  val empty = new TypedMap(immutable.Map.empty)

  /**
   * Builds a [[TypedMap]] from a list of keys and values.
   */
  def apply(entries: TypedEntry[_]*): TypedMap = {
    TypedMap.empty.+(entries: _*)
  }
  /**
   * Builds a [[TypedMap]] from a list of entries (key/value pairs). This method
   * is like `apply` but it can be used in varargs style from Java.
   */
  @varargs def withEntries(entries: TypedEntry[_]*): TypedMap = {
    apply(entries: _*)
  }
}

/**
 * A TypedKey is a key that can be used to get and set values in a
 * [[TypedMap]] or any object with typed keys. This class uses reference
 * equality for comparisons, so each new instance is different key.
 *
 * @param displayName The name to display for this key or `null` if
 * no display name has been provided. This name is only used for debugging.
 * Keys with the same name are considered different keys.
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
  def -> (value: A): TypedEntry[A] = bindValue(value)

  override def toString: String = displayName.getOrElse(super.toString)
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

/**
 * A factory for creating [[TypedKey]]s. Usable from Java.
 */
// This class is usable from Java. It will probably not be needed
// once we upgrade to Scala 2.12 which will support Java 8's ability
// to add static methods to interfaces.
object TypedKeyFactory {
  /**
   * Creates a [[TypedKey]] without a name.
   *
   * @tparam A The type of value this key is associated with.
   * @return A fresh key.
   */
  def create[A](): TypedKey[A] = TypedKey.apply[A];

  /**
   * Creates a [[TypedKey]] with the given name.
   *
   * @param displayName The name to display when printing this key.
   * @tparam A The type of value this key is associated with.
   * @return A fresh key.
   */
  def create[A](displayName: String): TypedKey[A] = TypedKey.apply(displayName);
}

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
