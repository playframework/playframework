package play.api.mvc

import scala.annotation._

import play.api.mvc._

import java.net.{ URLEncoder, URLDecoder }
import scala.annotation._

import scala.collection.JavaConverters._

/**
 * Binder for query string parameters.
 * 
 * You can provide an implementation of `QueryStringBindable[A]` for any type `A` you want to be able to
 * bind directly from the request query string.
 * 
 * For example, if you have the following type to encode pagination:
 * 
 * {{{
 *   /**
 *    * @param index Current page index
 *    * @param size Number of items in a page
 *    */
 *   case class Pager(index: Int, size: Int)
 * }}}
 * 
 * Play will create a `Pager(5, 42)` value from a query string looking like `/foo?p.index=5&p.size=42` if you define
 * an instance of `QueryStringBindable[Pager]` available in the implicit scope.
 * 
 * For example:
 * 
 * {{{
 *   object Pager {
 *     implicit def queryStringBinder(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[Pager] {
 *       override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Pager]] = {
 *         for {
 *           index <- intBinder.bind(key + ".index", params)
 *           size <- intBinder.bind(key + ".size", params)
 *         } yield {
 *           (index, size) match {
 *             case (Right(index), Right(size)) => Right(Pager(index, size))
 *             case _ => Left("Unable to bind a Pager")
 *           }
 *         }
 *       }
 *       override def unbind(key: String, pager: Pager): String = {
 *         intBinder.unbind(key + ".index", pager.index) + "&" + intBinder.unbind(key + ".size", pager.size)
 *       }
 *     }
 *   }
 * }}}
 * 
 * To use it in a route, just write a type annotation aside the parameter you want to bind:
 * 
 * {{{
 *   GET  /foo        controllers.foo(p: Pager)
 * }}}
 */
@implicitNotFound(
  "No QueryString binder found for type ${A}. Try to implement an implicit QueryStringBindable for this type."
)
trait QueryStringBindable[A] {

  /**
   * Bind a query string parameter.
   *
   * @param key Parameter key
   * @param params QueryString data
   * @return `None` if the parameter was not present in the query string data. Otherwise, returns `Some` of either
   * `Right` of the parameter value, or `Left` of an error message if the binding failed.
   */
  def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, A]]

  /**
   * Unbind a query string parameter.
   *
   * @param key Parameter key
   * @param value Parameter value.
   * @return a query string fragment containing the key and its value. E.g. "foo=42"
   */
  def unbind(key: String, value: A): String

  /**
   * Javascript function to unbind in the Javascript router.
   */
  def javascriptUnbind: String = """function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)}"""

}

/**
 * Binder for URL path parameters.
 * 
 * You can provide an implementation of `PathBindable[A]` for any type `A` you want to be able to
 * bind directly from the request path.
 * 
 * For example, given this class definition:
 * 
 * {{{
 *   case class User(id: Int, name: String, age: Int)
 * }}}
 * 
 * You can define a binder retrieving a `User` instance from its id, useable like the following:
 * 
 * {{{
 *   // In your routes:
 *   // GET  /show/:user      controllers.Application.show(user)
 *   // For example: /show/42
 *   
 *   object Application extends Controller {
 *     def show(user: User) = Action {
 *       …
 *     }
 *   }
 * }}}
 * 
 * The definition the binder can look like the following:
 * 
 * {{{
 *   object User {
 *     implicit def pathBinder(implicit intBinder: QueryStringBindable[Int]) = new PathBindable[User] {
 *       override def bind(key: String, value: String): Either[String, User] = {
 *         for {
 *           id <- intBinder.bind(key, value).right
 *           user <- User.findById(id).toRight("User not found").right
 *         } yield user
 *       }
 *       override def unbind(key: String, user: User): String = {
 *         intBinder.unbind(user.id)
 *       }
 *     }
 *   }
 * }}}
 */
@implicitNotFound(
  "No URL path binder found for type ${A}. Try to implement an implicit PathBindable for this type."
)
trait PathBindable[A] {

  /**
   * Bind an URL path parameter.
   *
   * @param key Parameter key
   * @param value The value as String (extracted from the URL path)
   * @return `Right` of the value or `Left` of an error message if the binding failed
   */
  def bind(key: String, value: String): Either[String, A]

  /**
   * Unbind a URL path  parameter.
   *
   * @param key Parameter key
   * @param value Parameter value.
   */
  def unbind(key: String, value: A): String

  /**
   * Javascript function to unbind in the Javascript router.
   */
  def javascriptUnbind: String = """function(k,v) {return v}"""

}

/**
 * Transform a value to a Javascript literal.
 */
@implicitNotFound(
  "No JavaScript litteral binder found for type ${A}. Try to implement an implicit JavascriptLitteral for this type."
)
trait JavascriptLitteral[A] {

  /**
   * Convert a value of A to a JavaScript literal.
   */
  def to(value: A): String

}

/**
 * Default JavaScript literals converters.
 */
object JavascriptLitteral {

  /**
   * Convert a Scala String to Javascript String
   */
  implicit def litteralString = new JavascriptLitteral[String] {
    def to(value: String) = "\"" + value + "\""
  }

  /**
   * Convert a Scala Int to Javascript number
   */
  implicit def litteralInt = new JavascriptLitteral[Int] {
    def to(value: Int) = value.toString
  }

  /**
   * Convert a Java Integer to Javascript number
   */
  implicit def litteralInteger = new JavascriptLitteral[java.lang.Integer] {
    def to(value: java.lang.Integer) = value.toString
  }

  /**
   * Convert a Scala Long to Javascript Long
   */
  implicit def litteralLong = new JavascriptLitteral[Long] {
    def to(value: Long) = value.toString
  }

  /**
   * Convert a Scala Boolean to Javascript boolean
   */
  implicit def litteralBoolean = new JavascriptLitteral[Boolean] {
    def to(value: Boolean) = value.toString
  }

  /**
   * Convert a Scala Option to Javascript literal (use null for None)
   */
  implicit def litteralOption[T](implicit jsl: JavascriptLitteral[T]) = new JavascriptLitteral[Option[T]] {
    def to(value: Option[T]) = value.map(jsl.to(_)).getOrElse("null")
  }

}

/**
 * Default binders for Query String
 */
object QueryStringBindable {

  /**
   * QueryString binder for String.
   */
  implicit def bindableString = new QueryStringBindable[String] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map(Right(_)) // No need to URL decode from query string since netty already does that
    def unbind(key: String, value: String) = key + "=" + (URLEncoder.encode(value, "utf-8"))
  }

  /**
   * QueryString binder for Int.
   */
  implicit def bindableInt = new QueryStringBindable[Int] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { i =>
      try {
        Right(java.lang.Integer.parseInt(i))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Int: " + e.getMessage)
      }
    }
    def unbind(key: String, value: Int) = key + "=" + value.toString
  }

  /**
   * QueryString binder for Long.
   */
  implicit def bindableLong = new QueryStringBindable[Long] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { i =>
      try {
        Right(java.lang.Long.parseLong(i))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Long: " + e.getMessage)
      }
    }
    def unbind(key: String, value: Long) = key + "=" + value.toString
  }

  /**
   * QueryString binder for Integer.
   */
  implicit def bindableInteger = new QueryStringBindable[java.lang.Integer] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { i =>
      try {
        Right(java.lang.Integer.parseInt(i))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Integer: " + e.getMessage)
      }
    }
    def unbind(key: String, value: java.lang.Integer) = key + "=" + value.toString
  }

  /**
   * QueryString binder for Boolean.
   */
  implicit def bindableBoolean = new QueryStringBindable[Boolean] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { i =>
      try {
        java.lang.Integer.parseInt(i) match {
          case 0 => Right(false)
          case 1 => Right(true)
          case _ => Left("Cannot parse parameter " + key + " as Boolean: should be 0 or 1")
        }
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Boolean: should be 0 or 1")
      }
    }
    def unbind(key: String, value: Boolean) = key + "=" + (if (value) "1" else "0")
  }

  /**
   * QueryString binder for Option.
   */
  implicit def bindableOption[T: QueryStringBindable] = new QueryStringBindable[Option[T]] {
    def bind(key: String, params: Map[String, Seq[String]]) = {
      Some(
        implicitly[QueryStringBindable[T]].bind(key, params)
          .map(_.right.map(Some(_)))
          .getOrElse(Right(None)))
    }
    def unbind(key: String, value: Option[T]) = value.map(implicitly[QueryStringBindable[T]].unbind(key, _)).getOrElse("")
  }

  /**
   * QueryString binder for Java Option.
   */
  implicit def bindableJavaOption[T: QueryStringBindable] = new QueryStringBindable[play.libs.F.Option[T]] {
    def bind(key: String, params: Map[String, Seq[String]]) = {
      Some(
        implicitly[QueryStringBindable[T]].bind(key, params)
          .map(_.right.map(play.libs.F.Option.Some(_)))
          .getOrElse(Right(play.libs.F.Option.None.asInstanceOf[play.libs.F.Option[T]])))
    }
    def unbind(key: String, value: play.libs.F.Option[T]) = {
      if (value.isDefined) {
        implicitly[QueryStringBindable[T]].unbind(key, value.get)
      } else {
        ""
      }
    }
  }

  /**
   * QueryString binder for QueryStringBindable.
   */
  implicit def javaQueryStringBindable[T <: play.mvc.QueryStringBindable[T]](implicit m: Manifest[T]) = new QueryStringBindable[T] {
    def bind(key: String, params: Map[String, Seq[String]]) = {
      try {
        val o = m.erasure.newInstance.asInstanceOf[T].bind(key, params.mapValues(_.toArray).asJava)
        if (o.isDefined) {
          Some(Right(o.get))
        } else {
          None
        }
      } catch {
        case e => Some(Left(e.getMessage))
      }
    }
    def unbind(key: String, value: T) = {
      value.unbind(key)
    }
  }

}

/**
 * Default binders for URL path part.
 */
object PathBindable {

  /**
   * Path binder for String.
   */
  implicit def bindableString = new PathBindable[String] {
    def bind(key: String, value: String) = Right(URLDecoder.decode(value, "utf-8"))
    def unbind(key: String, value: String) = value
  }

  /**
   * Path binder for Int.
   */
  implicit def bindableInt = new PathBindable[Int] {
    def bind(key: String, value: String) = {
      try {
        Right(java.lang.Integer.parseInt(URLDecoder.decode(value, "utf-8")))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Int: " + e.getMessage)
      }
    }
    def unbind(key: String, value: Int) = value.toString
  }

  /**
   * Path binder for Long.
   */
  implicit def bindableLong = new PathBindable[Long] {
    def bind(key: String, value: String) = {
      try {
        Right(java.lang.Long.parseLong(URLDecoder.decode(value, "utf-8")))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Long: " + e.getMessage)
      }
    }
    def unbind(key: String, value: Long) = value.toString
  }

  /**
   * Path binder for Integer.
   */
  implicit def bindableInteger = new PathBindable[java.lang.Integer] {
    def bind(key: String, value: String) = {
      try {
        Right(java.lang.Integer.parseInt(URLDecoder.decode(value, "utf-8")))
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Integer: " + e.getMessage)
      }
    }
    def unbind(key: String, value: java.lang.Integer) = value.toString
  }

  /**
   * Path binder for Boolean.
   */
  implicit def bindableBoolean = new PathBindable[Boolean] {
    def bind(key: String, value: String) = {
      try {
        java.lang.Integer.parseInt(URLDecoder.decode(value, "utf-8")) match {
          case 0 => Right(false)
          case 1 => Right(true)
          case _ => Left("Cannot parse parameter " + key + " as Boolean: should be 0 or 1")
        }
      } catch {
        case e: NumberFormatException => Left("Cannot parse parameter " + key + " as Boolean: should be 0 or 1")
      }
    }
    def unbind(key: String, value: Boolean) = key + "=" + (if (value) "1" else "0")
  }

  /**
   * Path binder for Option.
   */
  implicit def bindableOption[T: PathBindable] = new PathBindable[Option[T]] {
    def bind(key: String, value: String) = {
      implicitly[PathBindable[T]].bind(key, value).right.map(Some(_))
    }
    def unbind(key: String, value: Option[T]) = value.map(v => implicitly[PathBindable[T]].unbind(key, v)).getOrElse("")
  }

  /**
   * Path binder for Java Option.
   */
  implicit def javaPathBindable[T <: play.mvc.PathBindable[T]](implicit m: Manifest[T]) = new PathBindable[T] {
    def bind(key: String, value: String) = {
      try {
        Right(m.erasure.newInstance.asInstanceOf[T].bind(key, value))
      } catch {
        case e => Left(e.getMessage)
      }
    }
    def unbind(key: String, value: T) = {
      value.unbind(key)
    }
  }

}
