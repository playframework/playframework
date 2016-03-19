package play.core.server

import scala.reflect.ClassTag

/**
 * A container to store components used by the server that it may also want to make available to the application.
 *
 * Applications MUST be able to run normally in the absence of any server components.
 *
 * @param components the set of components
 */
case class ServerComponents(components: Map[Class[_], Any] = Map.empty) {

  def this() = this(Map.empty)

  /**
   * Get an instance of the given class, throwing a [[NoSuchElementException]] if the instance is not found.
   */
  private[server] def apply[T](implicit ct: ClassTag[T]): T =
    get(ct).getOrElse(throw new NoSuchElementException(s"No server component bound for ${ct.runtimeClass.getName}"))

  /**
   * Optionally get an instance of the given class.
   */
  def get[T](implicit ct: ClassTag[T]): Option[T] = get(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Optionally get an instance of the given class
   */
  def get[T](clazz: Class[T]): Option[T] = components.get(clazz).asInstanceOf[Option[T]]

  /**
   * Add a component to the server components.
   */
  def +[T](component: T)(implicit ct: ClassTag[T]) = copy(components = components + (ct.runtimeClass -> component))
}
