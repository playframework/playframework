/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.typedmap

import scala.annotation.varargs
import scala.collection.immutable

/**
 * A PropMap is an immutable map containing [[TypedKey]]s and their values.
 *
 * @param m The map used to store values.
 */
final class TypedMap private (m: immutable.Map[TypedKey[_], Any]) {
  def apply[A](prop: TypedKey[A]): A = m.apply(prop).asInstanceOf[A]
  def get[A](prop: TypedKey[A]): Option[A] = m.get(prop).asInstanceOf[Option[A]]
  def getOrElse[A](prop: TypedKey[A], default: => A): A = m.getOrElse(prop, default).asInstanceOf[A]
  def contains[A](p: TypedKey[A]): Boolean = m.contains(p)
  def updated[A](prop: TypedKey[A], value: A): TypedMap = new TypedMap(m.updated(prop, value))
  def +(entry: TypedEntry[_]): TypedMap = {
    // Use `helper` to bind the key and value to the same type variable, A
    def helper[A](e: TypedEntry[A]): TypedMap = updated(e.key, e.value)
    helper(entry)
  }
  def +(entry: TypedEntry[_], entries: TypedEntry[_]*): TypedMap = {
    val m1 = entries.foldLeft(m.updated(entry.key, entry.value)) {
      case (m, e) => m.updated(e.key, e.value)
    }
    new TypedMap(m1)
  }
  override def toString: String = m.mkString
}

object TypedMap {

  /**
   * Create an empty [[TypedMap]].
   */
  val empty = new TypedMap(immutable.Map.empty)

  /**
   * Builds a [[TypedMap]] from a list of props and values.
   */
  def apply(entry: TypedEntry[_]): TypedMap = TypedMap.empty + entry
  def apply(entry: TypedEntry[_], entries: TypedEntry[_]*): TypedMap = {
    TypedMap.empty.+(entry, entries: _*)
  }

  /**
   * Builds a [[TypedMap]] from a list of props and values. This method
   * is like `apply` but it can be used in varargs style from Java.
   */
  def withEntries(entry: TypedEntry[_]): TypedMap = apply(entry)
  @varargs def withEntries(entry: TypedEntry[_], entries: TypedEntry[_]*): TypedMap = {
    apply(entry, entries: _*)
  }
}

/**
 * A property is a key that can be used to get and set values on an
 * object that implements [[HasProps]].
 *
 * @tparam A The type of values associated with this property.
 */
trait TypedKey[A] {

  /**
   * Bind this property to a value.
   *
   * @param value The value to bind this property to.
   * @return A bound value.
   */
  def bindValue(value: A): TypedEntry[A] = TypedEntry(this, value)

  /**
   * Bind this property to a value.
   *
   * @param value
   * @return
   */
  def ->(value: A): TypedEntry[A] = bindValue(value)
}

/**
 * Helper for working with `Prop`s.
 */
object TypedKey {
  /**
   *
   * @param displayName
   * @tparam A
   * @return
   */
  def apply[A](displayName: String): TypedKey[A] = new TypedKey[A] {
    override def toString: String = displayName
  }
}

final case class TypedEntry[A](key: TypedKey[A], value: A) {
  def toPair: (TypedKey[A], A) = (key, value)
}
