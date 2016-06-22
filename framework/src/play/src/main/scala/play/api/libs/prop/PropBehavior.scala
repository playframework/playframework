package play.api.libs.prop

/**
 * A `PropBehavior` describes the way that properties are accessed and updated
 * in a [[HasProps]] object. By implementing a `Behavior` it is possible to implement
 * things like default values, lazy values, etc. You can use composition to
 * extend one behavior with another.
 */
trait PropBehavior {
  def doGet[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, A)
  def doContains[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, Boolean)
  def doUpdate[A](behavior: PropBehavior, state: PropState, p: Prop[A], v: A): PropState

  /**
   * Get some value if it exists or none otherwise. Subclasses may wish to override
   * this with a more efficient implementation.
   */
  def doOptGet[A](self: PropBehavior, state: PropState, p: Prop[Any]): (PropState, Option[Any]) = {
    val (afterContainsState, contains) = doContains(self, state, p)
    if (contains) {
      val (afterGetState, value) = doGet(self, afterContainsState, p)
      (afterGetState, Some(value))
    } else {
      (afterContainsState, None)
    }
  }
}

object PropBehavior {

  /**
   * This behavior has no logic, it simply loads and stores from the state.
   */
  object Basic extends PropBehavior {
    override def doGet[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, A) =
      (state, state(p))
    override def doContains[A](behavior: PropBehavior, state: PropState, p: Prop[A]): (PropState, Boolean) =
      (state, state.contains(p))
    override def doUpdate[A](behavior: PropBehavior, state: PropState, p: Prop[A], v: A): PropState =
      state.update(p, v)
    override def doOptGet[A](self: PropBehavior, state: PropState, p: Prop[Any]): (PropState, Option[Any]) = {
      (state, state.get(p))
    }
  }
}