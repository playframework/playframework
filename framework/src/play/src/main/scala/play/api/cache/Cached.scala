package play.api.cache

import play.api._
import play.api.mvc._

/**
 * Cache an action.
 *
 * @param key Compute a key from the request header
 * @param duration Cache duration (in seconds)
 * @param action Action to cache
 */
case class Cached[A](key: RequestHeader => String, duration: Int)(action: Action[A, Request])(implicit app: Application) extends Action[A, Request] {

  lazy val parser = action.parser

  def req[A] = (rh: RequestHeader, a:A) => Request(rh,a)

  def apply(request: Request[A]): Result = {
    Cache.getOrElse[Result](key(request), duration) {
      action(request)
    }(app, implicitly[Manifest[Result]])
  }

}

object Cached {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param action Action to cache
   */
  def apply[A](key: RequestHeader => String)(action: Action[A, Request])(implicit app: Application): Cached[A] = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param action Action to cache
   */
  def apply[A](key: String)(action: Action[A, Request])(implicit app: Application): Cached[A] = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   * @param action Action to cache
   */
  def apply[A](key: String, duration: Int)(action: Action[A, Request])(implicit app: Application): Cached[A] = {
    Cached(_ => key, duration)(action)
  }

}