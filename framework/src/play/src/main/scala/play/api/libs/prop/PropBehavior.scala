package play.api.libs.prop

/**
 * A `PropBehavior` describes the way that properties are accessed and updated
 * in a [[HasProps]] object. By implementing a `Behavior` it is possible to implement
 * things like default values, lazy values, etc. You can use composition to
 * extend one behavior with another.
 */
trait PropBehavior {
  // All methods are prefixed with 'propBehavior' to allow easy mixing in
  def doGet[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, A)
  def doContains[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, Boolean)
  def doUpdate[A](behavior: PropBehavior, state: PropState, p: Prop[A], v: A): PropState
}

object PropBehavior {
  object Basic extends PropBehavior {
    override def doGet[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, A) =
      (state, state(p))
    override def doContains[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, Boolean) =
      (state, state.contains(p))
    override def doUpdate[A](behavior: PropBehavior, state: PropState, p: Prop[A], v: A): PropState =
      state.update(p, v)
  }
}