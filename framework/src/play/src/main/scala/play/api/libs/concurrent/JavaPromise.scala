package play.api.libs.concurrent

abstract class Recover[+T] extends PartialFunction[Throwable, T] {
  override final def isDefinedAt(t: Throwable): Boolean = true
  override final def apply(t: Throwable): T = recover(t)
  @throws(classOf[Throwable])
  def recover(failure: Throwable): T
}
