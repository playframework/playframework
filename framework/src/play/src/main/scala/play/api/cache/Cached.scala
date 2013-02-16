package play.api.cache

import play.api._
import play.api.mvc._
import scala.concurrent.duration.Duration
import reflect.ClassTag

/**
 * Cache an action, with duration in seconds.
 *
 * @param key Compute a key from the request header
 * @param duration Cache duration (in seconds)
 * @param action Action to cache
 */
case class Cached[A](key: RequestHeader => String, duration: Int)(action: Action[A])(implicit app: Application) extends Action[A] {

  lazy val parser = action.parser

  def apply(request: Request[A]): Result = {
    Cache.getOrElse[Result](key(request), duration) {
      action(request)
    }(app, implicitly[ClassTag[Result]])
  }

}

object Cached {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param action Action to cache
   */
  def apply[A](key: RequestHeader => String)(action: Action[A])(implicit app: Application): Cached[A] = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param action Action to cache
   */
  def apply[A](key: String)(action: Action[A])(implicit app: Application): Cached[A] = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.  Use Duration.Inf for infinite cache.
   *
   * @param key Cache key
   * @param duration Cache duration (as a Scala Duration, rounded to seconds)
   * @param action Action to cache
   */
  def apply[A](key: String, duration: Duration)(action: Action[A])(implicit app: Application): Cached[A] = {
    apply(key, duration = Cache.durationToExpiration(duration))(action)
  }

  /**
   * Cache an action.  Duration of 0 is Infinite
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   * @param action Action to cache
   */
  def apply[A](key: String, duration: Int)(action: Action[A])(implicit app: Application): Cached[A] = {
    Cached(_ => key, duration)(action)
  }

}