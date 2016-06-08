/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.prop

/**
 * An object that contains properties. Each property uses a [[Prop]]
 * as a key. `Prop`s identify each property and also define its type.
 *
 * @tparam Repr The concrete representation of the `HasProps`. This
 *              type is used when constructing a new instance.
 */
trait HasProps[+Repr] {

  protected def propBehavior: HasProps.Behavior
  protected def propState: HasProps.State[Repr]

  def prop[A](p: Prop[A]): A = {
    val b = propBehavior
    val s = propState
    b.propBehaviorGet(b, s, p)
  }
  def propOrElse[A](p: Prop[A], default: => A): A = {
    val b = propBehavior
    val s = propState
    if (b.propBehaviorContains(b, s, p)) b.propBehaviorGet(b, s, p) else default
  }
  def withProp[A](p: Prop[A], v: A): Repr = {
    val b = propBehavior
    val s = propState
    b.propBehaviorUpdate(b, s, p, v).propStateToRepr
  }
  def containsProp[A](p: Prop[A]): Boolean = {
    val b = propBehavior
    val s = propState
    b.propBehaviorContains(b, s, p)
  }
  def withProps(ps: Prop.WithValue[_]*): Repr = {
    val b = propBehavior
    val s = propState
    val s2 = ps.foldLeft(s) { case (s1, p) => b.propBehaviorUpdate(b, s1, p.prop, p.value) }
    s2.propStateToRepr
  }
}

object HasProps {

  /**
   * A `Behavior` describes the way that properties are accessed and updated
   * in a [[HasProps]] object. By implementing a `Behavior` it is possible to implement
   * things like default values, lazy values, etc. You can use composition to
   * extend one behavior with another.
   */
  trait Behavior {
    // All methods are prefixed with 'propBehavior' to allow easy mixing in
    def propBehaviorGet[A](behavior: Behavior, state: State[_], p: Prop[A]): A
    def propBehaviorContains[A](behavior: Behavior, state: State[_], p: Prop[A]): Boolean
    def propBehaviorUpdate[Repr, A](behavior: Behavior, state: State[Repr], p: Prop[A], v: A): State[Repr]
  }

  /**
   * A `State` object stores the mutable state of a [[HasProps]] object.
   * A simple implementation is to use a [[PropMap]]. See [[WithMapState]].
   * You can also provide your own implementation, for example if you want
   * to store some properties in a more efficient way.
   *
   * @tparam Repr The type of the `HasProps` that this state is linked to.
   *              The `propStateToRepr` method converts this state to that
   *              representation.
   */
  trait State[+Repr] {
    // All methods are prefixed with 'propState' to allow easy mixing in
    def propStateGet[A](p: Prop[A]): A
    def propStateGetOrElse[A](p: Prop[A], default: => A): A
    def propStateContains[A](p: Prop[A]): Boolean
    def propStateUpdate[A](p: Prop[A], v: A): State[Repr]
    def propStateToRepr: Repr
    def propStateToString: String
  }

  /**
   * A [[HasProps]] and [[State]] implementation backed by a [[PropMap]].
   *
   * @tparam Repr The concrete representation of the `HasProps`. This
   *              type is used when constructing a new instance.
   */
  trait WithMapState[+Repr] extends HasProps[Repr] with State[Repr] {
    self: Repr =>
    protected def propMap: PropMap
    protected def newState(newMap: PropMap): State[Repr]

    override protected def propState: HasProps.State[Repr] = this
    override def propStateGet[A](p: Prop[A]): A = {
      propMap(p)
    }
    override def propStateGetOrElse[A](p: Prop[A], default: => A): A = {
      propMap.getOrElse(p, default)
    }
    override def propStateContains[A](p: Prop[A]): Boolean = {
      propMap.contains(p)
    }
    override def propStateUpdate[A](p: Prop[A], v: A): HasProps.State[Repr] = {
      newState(propMap.updated(p, v))
    }
    override def propStateToRepr: Repr = this
    override def propStateToString: String = propMap.toString
  }

}