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

  protected val propBehavior: PropBehavior
  protected var propState: PropState
  protected def withPropState(s: PropState): Repr

  final def prop[A](p: Prop[A]): A = {
    val (newState, value): (PropState, A) = propBehavior.doGet(propBehavior, propState, p)
    propState = newState
    value
  }
  final def propOrElse[A](p: Prop[A], default: => A): A = {
    if (containsProp(p)) { prop(p) } else { default }
  }
  final def withProp[A](p: Prop[A], v: A): Repr = {
    val newState = propBehavior.doUpdate(propBehavior, propState, p, v)
    withPropState(newState)
  }
  final def containsProp[A](p: Prop[A]): Boolean = {
    val (newState, contained) = propBehavior.doContains(propBehavior, propState, p)
    propState = newState
    contained
  }
  final def withProps(ps: Prop.WithValue[_]*): Repr = {
    val newState = ps.foldLeft(propState) {
      case (stateAcc, p) => propBehavior.doUpdate(propBehavior, stateAcc, p.prop, p.value)
    }
    withPropState(newState)
  }
}

abstract class DefaultHasProps[+Repr](
  override protected val propBehavior: PropBehavior,
  override protected var propState: PropState) extends HasProps[Repr]
