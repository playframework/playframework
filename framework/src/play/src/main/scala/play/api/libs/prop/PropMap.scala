/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.prop

import scala.annotation.varargs
import scala.collection.immutable

/**
 * A PropMap is an immutable map containing [[Prop]]s and their values.
 *
 * @param m The map used to store values.
 */
class PropMap private (m: immutable.Map[Prop[_], Any]) extends PropState {
  override def contains[A](p: Prop[A]): Boolean = m.contains(p)
  override def update[A](prop: Prop[A], value: A): PropMap = new PropMap(m.updated(prop, value))
  override def apply[A](prop: Prop[A]): A = m.apply(prop).asInstanceOf[A]
  override def get[A](prop: Prop[A]): Option[A] = m.get(prop).asInstanceOf[Option[A]]
  override def getOrElse[A](prop: Prop[A], default: => A): A = m.getOrElse(prop, default).asInstanceOf[A]
  override def toString: String = m.mkString

  def update(propsWithValues: Prop.WithValue[_]*): PropMap = {
    val m1 = propsWithValues.foldLeft(m) { case (m, pwv) => m.updated(pwv.prop, pwv.value) }
    new PropMap(m1)
  }
}

object PropMap {

  /**
   * Create an empty [[PropMap]].
   */
  val empty = new PropMap(immutable.Map.empty)

  /**
   * Builds a [[PropMap]] from a list of props and values.
   */
  def apply(ps: Prop.WithValue[_]*): PropMap = PropMap.empty.update(ps: _*)

  /**
   * Builds a [[PropMap]] from a list of props and values. This method
   * is like `apply` but it can be used in varargs style from Java.
   */
  @varargs def withProps(ps: Prop.WithValue[_]*): PropMap = apply(ps: _*)
}