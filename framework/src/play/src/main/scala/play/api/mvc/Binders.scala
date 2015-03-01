/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import scala.annotation._

import play.api.mvc._

import controllers.Assets.Asset

import java.net.{ URI, URLEncoder }
import java.util.UUID
import scala.annotation._

import scala.collection.JavaConverters._
import reflect.ClassTag

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
  self =>

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

  /**
   * Transform this QueryStringBindable[A] to QueryStringBindable[B]
   */
  def transform[B](toB: A => B, toA: B => A) = new QueryStringBindable[B] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, B]] = {
      self.bind(key, params).map(_.right.map(toB))
    }
    def unbind(key: String, value: B): String = self.unbind(key, toA(value))
    override def javascriptUnbind: String = self.javascriptUnbind
  }
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
 *       ...
 *     }
 *   }
 * }}}
 *
 * The definition of binder can look like the following:
 *
 * {{{
 *   object User {
 *     implicit def pathBinder(implicit intBinder: PathBindable[Int]) = new PathBindable[User] {
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
  self =>

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

  /**
   * Transform this PathBinding[A] to PathBinding[B]
   */
  def transform[B](toB: A => B, toA: B => A) = new PathBindable[B] {
    def bind(key: String, value: String): Either[String, B] = self.bind(key, value).right.map(toB)
    def unbind(key: String, value: B): String = self.unbind(key, toA(value))
  }
}

/**
 * Transform a value to a Javascript literal.
 */
@implicitNotFound(
  "No JavaScript literal binder found for type ${A}. Try to implement an implicit JavascriptLiteral for this type."
)
trait JavascriptLiteral[A] {

  /**
   * Convert a value of A to a JavaScript literal.
   */
  def to(value: A): String

}

/**
 * Default JavaScript literals converters.
 */
object JavascriptLiteral {

  /**
   * Convert a (primitive) value to it's Javascript equivalent
   */
  private def toJsValue(value: Any): String = {
    value match {
      case null => "null"
      case _ => value.toString
    }
  }

  /**
   * Convert a value to a Javascript String
   */
  private def toJsString(value: Any): String = {
    value match {
      case null => "null"
      case _ => "\"" + value.toString + "\""
    }
  }

  /**
   * Convert a Scala String to Javascript String (or Javascript null if given String value is null)
   */
  implicit def literalString: JavascriptLiteral[String] = new JavascriptLiteral[String] {
    def to(value: String) = toJsString(value)
  }

  /**
   * Convert a Scala Int to Javascript number
   */
  implicit def literalInt: JavascriptLiteral[Int] = new JavascriptLiteral[Int] {
    def to(value: Int) = value.toString
  }

  /**
   * Convert a Java Integer to Javascript number (or Javascript null if given Integer value is null)
   */
  implicit def literalJavaInteger: JavascriptLiteral[java.lang.Integer] = new JavascriptLiteral[java.lang.Integer] {
    def to(value: java.lang.Integer) = toJsValue(value)
  }

  /**
   * Convert a Scala Long to Javascript Long
   */
  implicit def literalLong: JavascriptLiteral[Long] = new JavascriptLiteral[Long] {
    def to(value: Long) = value.toString
  }

  /**
   * Convert a Java Long to Javascript number (or Javascript null if given Long value is null)
   */
  implicit def literalJavaLong: JavascriptLiteral[java.lang.Long] = new JavascriptLiteral[java.lang.Long] {
    def to(value: java.lang.Long) = toJsValue(value)
  }

  /**
   * Convert a Scala Boolean to Javascript boolean
   */
  implicit def literalBoolean: JavascriptLiteral[Boolean] = new JavascriptLiteral[Boolean] {
    def to(value: Boolean) = value.toString
  }

  /**
   * Convert a Java Boolean to Javascript boolean (or Javascript null if given Boolean value is null)
   */
  implicit def literalJavaBoolean: JavascriptLiteral[java.lang.Boolean] = new JavascriptLiteral[java.lang.Boolean] {
    def to(value: java.lang.Boolean) = toJsValue(value)
  }

  /**
   * Convert a Scala Option to Javascript literal (use null for None)
   */
  implicit def literalOption[T](implicit jsl: JavascriptLiteral[T]): JavascriptLiteral[Option[T]] = new JavascriptLiteral[Option[T]] {
    def to(value: Option[T]) = value.map(jsl.to(_)).getOrElse("null")
  }

  /**
   * Convert a Java Option to Javascript literal (use null for None)
   */
  implicit def literalJavaOption[T](implicit jsl: JavascriptLiteral[T]): JavascriptLiteral[play.libs.F.Option[T]] = new JavascriptLiteral[play.libs.F.Option[T]] {
    def to(value: play.libs.F.Option[T]) = {
      if (value.isDefined) {
        jsl.to(value.get)
      } else {
        "null"
      }
    }
  }

  /**
   * Convert a Play Asset to Javascript String
   */
  implicit def literalAsset: JavascriptLiteral[Asset] = new JavascriptLiteral[Asset] {
    def to(value: Asset) = toJsString(value.name)
  }

  /**
   * Convert a java.util.UUID to Javascript String (or Javascript null if given UUID value is null)
   */
  implicit def literalUUID: JavascriptLiteral[UUID] = new JavascriptLiteral[UUID] {
    def to(value: UUID) = toJsString(value)
  }
}

/**
 * Default binders for Query String
 */
object QueryStringBindable {

  class Parsing[A](parse: String => A, serialize: A => String, error: (String, Exception) => String)
      extends QueryStringBindable[A] {

    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { p =>
      try {
        Right(parse(p))
      } catch {
        case e: Exception => Left(error(key, e))
      }
    }
    def unbind(key: String, value: A) = key + "=" + serialize(value)
  }

  /**
   * QueryString binder for String.
   */
  implicit def bindableString = new QueryStringBindable[String] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map(Right(_)) // No need to URL decode from query string since netty already does that
    // Use an option here in case users call index(null) in the routes -- see #818
    def unbind(key: String, value: String) = key + "=" + URLEncoder.encode(Option(value).getOrElse(""), "utf-8")
  }

  /**
   * QueryString binder for Int.
   */
  implicit object bindableInt extends Parsing[Int](
    _.toInt, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Int: %s".format(key, e.getMessage)
  )

  /**
   * QueryString binder for Integer.
   */
  implicit def bindableJavaInteger: QueryStringBindable[java.lang.Integer] =
    bindableInt.transform(i => i, i => i)

  /**
   * QueryString binder for Long.
   */
  implicit object bindableLong extends Parsing[Long](
    _.toLong, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Long: %s".format(key, e.getMessage)
  )

  /**
   * QueryString binder for Java Long.
   */
  implicit def bindableJavaLong: QueryStringBindable[java.lang.Long] =
    bindableLong.transform(l => l, l => l)

  /**
   * QueryString binder for Double.
   */
  implicit object bindableDouble extends Parsing[Double](
    _.toDouble, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Double: %s".format(key, e.getMessage)
  )

  /**
   * QueryString binder for Java Double.
   */
  implicit def bindableJavaDouble: QueryStringBindable[java.lang.Double] =
    bindableDouble.transform(d => d, d => d)

  /**
   * QueryString binder for Float.
   */
  implicit object bindableFloat extends Parsing[Float](
    _.toFloat, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Float: %s".format(key, e.getMessage)
  )

  /**
   * QueryString binder for Java Float.
   */
  implicit def bindableJavaFloat: QueryStringBindable[java.lang.Float] =
    bindableFloat.transform(f => f, f => f)

  /**
   * QueryString binder for Boolean.
   */
  implicit object bindableBoolean extends Parsing[Boolean](
    _.trim match {
      case "true" => true
      case "false" => false
      case b => b.toInt match {
        case 1 => true
        case 0 => false
      }
    }, _.toString,
    (key: String, e: Exception) => "Cannot parse parameter %s as Boolean: should be true, false, 0 or 1".format(key)
  ) {
    override def javascriptUnbind = """function(k,v){return k+'='+(!!v)}"""
  }

  /**
   * QueryString binder for Java Boolean.
   */
  implicit def bindableJavaBoolean: QueryStringBindable[java.lang.Boolean] =
    bindableBoolean.transform(b => b, b => b)

  /**
   * QueryString binder for java.util.UUID.
   */
  implicit object bindableUUID extends Parsing[UUID](
    UUID.fromString(_), _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as UUID: %s".format(key, e.getMessage)
  )

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
    override def javascriptUnbind = javascriptUnbindOption(implicitly[QueryStringBindable[T]].javascriptUnbind)
  }

  /**
   * QueryString binder for Java Option.
   */
  implicit def bindableJavaOption[T: QueryStringBindable]: QueryStringBindable[play.libs.F.Option[T]] = new QueryStringBindable[play.libs.F.Option[T]] {
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
    override def javascriptUnbind = javascriptUnbindOption(implicitly[QueryStringBindable[T]].javascriptUnbind)
  }

  private def javascriptUnbindOption(jsUnbindT: String) = "function(k,v){return v!=null?(" + jsUnbindT + ")(k,v):''}"

  /**
   * QueryString binder for Seq
   */
  implicit def bindableSeq[T: QueryStringBindable]: QueryStringBindable[Seq[T]] = new QueryStringBindable[Seq[T]] {
    def bind(key: String, params: Map[String, Seq[String]]) = bindSeq[T](key, params)
    def unbind(key: String, values: Seq[T]) = unbindSeq(key, values)
    override def javascriptUnbind = javascriptUnbindSeq(implicitly[QueryStringBindable[T]].javascriptUnbind)
  }

  /**
   * QueryString binder for List
   */
  implicit def bindableList[T: QueryStringBindable]: QueryStringBindable[List[T]] =
    bindableSeq[T].transform(_.toList, _.toSeq)

  /**
   * QueryString binder for java.util.List
   */
  implicit def bindableJavaList[T: QueryStringBindable]: QueryStringBindable[java.util.List[T]] = new QueryStringBindable[java.util.List[T]] {
    def bind(key: String, params: Map[String, Seq[String]]) = bindSeq[T](key, params).map(_.right.map(_.asJava))
    def unbind(key: String, values: java.util.List[T]) = unbindSeq(key, values.asScala)
    override def javascriptUnbind = javascriptUnbindSeq(implicitly[QueryStringBindable[T]].javascriptUnbind)
  }

  private def bindSeq[T: QueryStringBindable](key: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[T]]] = {
    @tailrec
    def collectResults(values: List[String], results: List[T]): Either[String, Seq[T]] = {
      values match {
        case Nil => Right(results.reverse) // to preserve the original order
        case head :: rest =>
          implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(head))) match {
            case None => collectResults(rest, results)
            case Some(Right(result)) => collectResults(rest, result :: results)
            case Some(Left(err)) => collectErrs(rest, err :: Nil)
          }
      }
    }

    @tailrec
    def collectErrs(values: List[String], errs: List[String]): Left[String, Seq[T]] = {
      values match {
        case Nil => Left(errs.reverse.mkString("\n"))
        case head :: rest =>
          implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(head))) match {
            case Some(Left(err)) => collectErrs(rest, err :: errs)
            case Some(Right(_)) | None => collectErrs(rest, errs)
          }
      }
    }

    params.get(key) match {
      case None => Some(Right(Nil))
      case Some(values) => Some(collectResults(values.toList, Nil))
    }
  }

  private def unbindSeq[T: QueryStringBindable](key: String, values: Iterable[T]): String = {
    (for (value <- values) yield {
      implicitly[QueryStringBindable[T]].unbind(key, value)
    }).mkString("&")
  }

  private def javascriptUnbindSeq(jsUnbindT: String) = "function(k,vs){var l=vs&&vs.length,r=[],i=0;for(;i<l;i++){r[i]=(" + jsUnbindT + ")(k,vs[i])}return r.join('&')}"

  /**
   * QueryString binder for QueryStringBindable.
   */
  implicit def javaQueryStringBindable[T <: play.mvc.QueryStringBindable[T]](implicit ct: ClassTag[T]) = new QueryStringBindable[T] {
    def bind(key: String, params: Map[String, Seq[String]]) = {
      try {
        val o = ct.runtimeClass.newInstance.asInstanceOf[T].bind(key, params.mapValues(_.toArray).asJava)
        if (o.isDefined) {
          Some(Right(o.get))
        } else {
          None
        }
      } catch {
        case e: Exception => Some(Left(e.getMessage))
      }
    }
    def unbind(key: String, value: T) = {
      value.unbind(key)
    }
    override def javascriptUnbind = Option(ct.runtimeClass.newInstance.asInstanceOf[T].javascriptUnbind())
      .getOrElse(super.javascriptUnbind)
  }

}

/**
 * Default binders for URL path part.
 */
object PathBindable {

  class Parsing[A](parse: String => A, serialize: A => String, error: (String, Exception) => String)(implicit codec: Codec)
      extends PathBindable[A] {

    def bind(key: String, value: String): Either[String, A] = {
      try {
        Right(parse(value))
      } catch {
        case e: Exception => Left(error(key, e))
      }
    }
    def unbind(key: String, value: A): String = serialize(value)
  }

  /**
   * Path binder for String.
   */
  implicit object bindableString extends Parsing[String](
    (s: String) => s, (s: String) => s, (key: String, e: Exception) => "Cannot parse parameter %s as String: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Int.
   */
  implicit object bindableInt extends Parsing[Int](
    _.toInt, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Int: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Java Integer.
   */
  implicit def bindableJavaInteger: PathBindable[java.lang.Integer] =
    bindableInt.transform(i => i, i => i)

  /**
   * Path binder for Long.
   */
  implicit object bindableLong extends Parsing[Long](
    _.toLong, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Long: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Java Long.
   */
  implicit def bindableJavaLong: PathBindable[java.lang.Long] =
    bindableLong.transform(l => l, l => l)

  /**
   * Path binder for Double.
   */
  implicit object bindableDouble extends Parsing[Double](
    _.toDouble, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Double: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Java Double.
   */
  implicit def bindableJavaDouble: PathBindable[java.lang.Double] =
    bindableDouble.transform(d => d, d => d)

  /**
   * Path binder for Float.
   */
  implicit object bindableFloat extends Parsing[Float](
    _.toFloat, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Float: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Java Float.
   */
  implicit def bindableJavaFloat: PathBindable[java.lang.Float] =
    bindableFloat.transform(f => f, f => f)

  /**
   * Path binder for Boolean.
   */
  implicit object bindableBoolean extends Parsing[Boolean](
    _.trim match {
      case "true" => true
      case "false" => false
      case b => b.toInt match {
        case 1 => true
        case 0 => false
      }
    }, _.toString,
    (key: String, e: Exception) => "Cannot parse parameter %s as Boolean: should be true, false, 0 or 1".format(key)
  ) {
    override def javascriptUnbind = """function(k,v){return !!v}"""
  }

  /**
   * Path binder for Java Boolean.
   */
  implicit def bindableJavaBoolean: PathBindable[java.lang.Boolean] =
    bindableBoolean.transform(b => b, b => b)

  /**
   * Path binder for java.util.UUID.
   */
  implicit object bindableUUID extends Parsing[UUID](
    UUID.fromString(_), _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as UUID: %s".format(key, e.getMessage)
  )

  /**
   * Path binder for Java PathBindable
   */
  implicit def javaPathBindable[T <: play.mvc.PathBindable[T]](implicit ct: ClassTag[T]): PathBindable[T] = new PathBindable[T] {
    def bind(key: String, value: String) = {
      try {
        Right(ct.runtimeClass.newInstance.asInstanceOf[T].bind(key, value))
      } catch {
        case e: Exception => Left(e.getMessage)
      }
    }
    def unbind(key: String, value: T) = {
      value.unbind(key)
    }
    override def javascriptUnbind = Option(ct.runtimeClass.newInstance.asInstanceOf[T].javascriptUnbind())
      .getOrElse(super.javascriptUnbind)
  }

  /**
   * This is used by the Java RouterBuilder DSL.
   */
  private[play] lazy val pathBindableRegister: Map[Class[_], PathBindable[_]] = {
    def register[T](implicit pb: PathBindable[T], ct: ClassTag[T]) = ct.runtimeClass -> pb
    Map(
      register[String],
      register[java.lang.Integer],
      register[java.lang.Long],
      register[java.lang.Double],
      register[java.lang.Float],
      register[java.lang.Boolean],
      register[UUID]
    )
  }

}
