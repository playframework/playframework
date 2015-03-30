package play.api.libs.json

/**
 * A trait representing a Json node which can be read as an arbitrary type A using a Reads[A]
 */
trait JsReadable extends Any {
  /**
   * Tries to convert the node into a T. An implicit Reads[T] must be defined.
   * Any error is mapped to None
   *
   * @return Some[T] if it succeeds, None if it fails.
   */
  def asOpt[T](implicit fjs: Reads[T]): Option[T] = validate(fjs).asOpt

  /**
   * Tries to convert the node into a T, throwing an exception if it can't. An implicit Reads[T] must be defined.
   */
  def as[T](implicit fjs: Reads[T]): T = validate(fjs).fold(
    valid = identity,
    invalid = e => throw new JsResultException(e)
  )

  /**
   * Transforms this node into a JsResult using provided Json transformer Reads[JsValue]
   */
  def transform[A <: JsValue](rds: Reads[A]): JsResult[A] = validate(rds)

  /**
   * Tries to convert the node into a JsResult[T] (Success or Error). An implicit Reads[T] must be defined.
   */
  def validate[T](implicit rds: Reads[T]): JsResult[T]
}
