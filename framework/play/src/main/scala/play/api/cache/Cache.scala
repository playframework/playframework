package play.api.cache

import scala.collection.mutable.{ Map => MMap, SynchronizedMap, HashMap }
import collection.JavaConverters._
import java.util.Calendar
import play.api.Application
import util.control.Exception.handling

/** Internal cache interface. */
trait CacheAPI {
  def set(key: String, value: Any, expiration: Int)
  def set(key: String, value: Any)
  def get[T](key: String)(implicit m: Manifest[T]): Option[T]
  def get[T](keys: String*)(implicit m: Manifest[T]): Map[String, Option[T]]
  def getAsJava(keys: String*): java.util.Map[String, AnyRef]
  def getAsJava(key: String): AnyRef
}

/** Basic internal implementation of the Cache API. */
class BasicCache extends CacheAPI {

  /** http://www.scala-lang.org/docu/files/collections-api/collections_11.html */
  private def makeMap: MMap[String, Tuple2[java.util.Date, Any]] =
    new HashMap[String, Tuple2[java.util.Date, Any]] with SynchronizedMap[String, Tuple2[java.util.Date, Any]]

  private lazy val cache = makeMap

  def set(key: String, value: Any) = set(key, value, 1800)

  def set(key: String, value: Any, expiration: Int) {
    if (value == null)
      cache -= key
    else {
      val cal = Calendar.getInstance()
      cal.add(Calendar.SECOND, expiration)
      cache += key -> (cal.getTime, value)
    }
  }

  def get[T](key: String)(implicit m: Manifest[T]): Option[T] = {
    val cal = Calendar.getInstance()
    val value = cache.get(key).filter(_._1.compareTo(cal.getTime) >= 0).headOption
    //delete a key only if it's expired
    if (value.isDefined) {
      val unboxed = value.get._2
      if (m.erasure.isAssignableFrom(unboxed.getClass)) Some(unboxed.asInstanceOf[T]) else None
    } else {
      cache -= key
      None
    }
  }

  def get[T](keys: String*)(implicit m: Manifest[T]): Map[String, Option[T]] = {
    keys.map(key => key -> get[T](key)).toMap[String, Option[T]]
  }

  def getAsJava(keys: String*): java.util.Map[String, AnyRef] = keys.map(key => key -> getAsJava(key)).toMap.asJava

  def getAsJava(key: String): AnyRef = {
    val cal = Calendar.getInstance()
    cache.get(key).filter(_._1.compareTo(cal.getTime) >= 0).map(_._2.asInstanceOf[AnyRef]) getOrElse {
      cache -= key
      null
    }
  }
}

/** Developer-facing Cache API, implementation is received from plugin. */
object Cache {

  /** The exception we are throwing in case of plugin issues. */
  private def error = throw new Exception("looks like the cache plugin was not properly registered. Make sure at least one CachePlugin implementation is enabled, otherwise these calls won't work")

  /** Sets a value with expiration.
   *
   * @param expiration expiration period in seconds
   */
  def set(key: String, value: AnyRef, expiration: Int)(implicit app: Application) = app.plugin[CachePlugin].map(_.api.set(key, value, expiration)).getOrElse(error)

  /** Sets a value with a default expiration of 1800 seconds, i.e. 30 minutes. */
  def set(key: String, value: AnyRef)(implicit app: Application) = app.plugin[CachePlugin].map(_.api.set(key, value)).getOrElse(error)

  /** Retrieves a key in a type-safe way. */
  def get[T](key: String)(implicit m: Manifest[T], app: Application): Option[T] = app.plugin[CachePlugin].map(_.api.get[T](key).asInstanceOf[Option[T]]).getOrElse(error)

  /** Retrieves multiple keys from the same type.
   *
   * @keys varargs
   * @return cache key-value pairs from homogeneous type
   */
  def get[T](keys: String*)(implicit m: Manifest[T], app: Application): Map[String, Option[T]] = app.plugin[CachePlugin].map(_.api.get[T](keys: _*).asInstanceOf[Map[String, Option[T]]]).getOrElse(error)

  /** Retrieves multiple values in an unsafe way for Java interoperability.
   *
   * This method takes an array instead of varargs to avoid ambiguous method calls in the case of varargs plus an implicit `app` parameter.
   *
   * @keys a Java array of string of keys
   * @return java.util.Map[String,java.langObject]
   */
  def getAsJava(keys: Array[String])(implicit app: Application): java.util.Map[String, AnyRef] = app.plugin[CachePlugin].map(_.api.getAsJava(keys: _*)).getOrElse(error)

  /** Retrieves a value in an unsafe way for Java interoperability.
   *
   * @return a Java object
   */
  def getAsJava(key: String)(implicit app: Application): AnyRef = app.plugin[CachePlugin].map(_.api.getAsJava(key)).getOrElse(error)
}
