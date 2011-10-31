package play.api.cache

import scala.collection.mutable.{ Map => MMap, SynchronizedMap, HashMap }
import collection.JavaConverters._
import java.util.Calendar
import play.api.Application
import util.control.Exception.handling
/**
 * internal cache interface
 */
trait CacheAPI {
  def set(key: String, value: Any, expiration: Int)
  def set(key: String, value: Any)
  def get[T](key: String)(implicit m: Manifest[T]): Option[T]
  def get[T](keys: String*)(implicit m: Manifest[T]): Map[String, Option[T]]
  def getAsJava(keys: String*): java.util.Map[String, AnyRef]
  def getAsJava(key: String): AnyRef
}

/**
 * basic internal implementation of Cache API
 */
class BasicCache extends CacheAPI {
  /**
   * http://www.scala-lang.org/docu/files/collections-api/collections_11.html
   */
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

/**
 * developer facing Cache API, implementation is recieved from plugin
 */
object Cache {

  /**
   * set a value with expiration
   * @key
   * @value
   * @expiration it's in seconds
   */
  def set(key: String, value: AnyRef, expiration: Int)(implicit app: Application) = app.plugin[CachePlugin].api.set(key, value, expiration)

  /**
   * set a value with default expiration of 1800 sec or 30 min
   * @key
   * @value
   * @value
   */
  def set(key: String, value: AnyRef)(implicit app: Application) = app.plugin[CachePlugin].api.set(key, value)

  /**
   * retrieves key in a typesafe way
   * @key
   * @return
   */
  def get[T](key: String)(implicit m: Manifest[T], app: Application): Option[T] = app.plugin[CachePlugin].api.get[T](key).asInstanceOf[Option[T]]

  /**
   * retrieves multiple keys from the same type
   * @keys varargs
   * @return cache key value pairs from homogeneous type
   */
  def get[T](keys: String*)(implicit m: Manifest[T], app: Application): Map[String, Option[T]] = app.plugin[CachePlugin].api.get[T](keys: _*).asInstanceOf[Map[String, Option[T]]]

  /**
   * retrieves multiple values in an unsafe way for java interop
   * it's taking an array instead of a varargs to avoid ambiguous method calls in case of varargs+ implicit app extra param
   * @keys takes an java array of string of keys
   * @return java.util.Map[String,java.langObject]
   */
  def getAsJava(keys: Array[String])(implicit app: Application): java.util.Map[String, AnyRef] = app.plugin[CachePlugin].api.getAsJava(keys: _*)

  /**
   * retrieves a value in an unsafe way for java interop
   * @key
   * @return java object
   */
  def getAsJava(key: String)(implicit app: Application): AnyRef = app.plugin[CachePlugin].api.getAsJava(key)
}
