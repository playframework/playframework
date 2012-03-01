package play.utils

import java.util.concurrent.{ TimeUnit, Callable }

/**
 * provides conversion helpers
 */
object Conversions {

  def newMap[A, B](data: (A, B)*) = Map(data: _*)

  def timeout[A](callable: Callable[A], duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): play.api.libs.concurrent.Promise[A] =
    play.api.libs.concurrent.Promise.timeout(callable.call(), duration, unit)

}
