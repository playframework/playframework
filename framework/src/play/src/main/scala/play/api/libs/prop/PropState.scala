package play.api.libs.prop

/**
 * A `PropState` object stores the mutable propState of a [[HasProps]] object.
 * A simple implementation is to use a [[PropMap]]. See [[WithMapState]].
 * You can also provide your own implementation, for example if you want
 * to store some properties in a more efficient way.
 *
 * @tparam Repr The type of the `HasProps` that this propState is linked to.
 *              The `propStateToRepr` method converts this propState to that
 *              representation.
 */
trait PropState {
  def apply[A](p: Prop[A]): A
  def get[A](p: Prop[A]): Option[A]
  def getOrElse[A](p: Prop[A], default: => A): A
  def contains[A](p: Prop[A]): Boolean
  def update[A](p: Prop[A], v: A): PropState
}