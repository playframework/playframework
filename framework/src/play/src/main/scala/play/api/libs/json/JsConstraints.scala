package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

//object Constraints extends ConstraintReads with ConstraintWrites

object ConstraintReads extends ConstraintReads

trait ConstraintReads {
  def of[T](implicit fmt: Format[T]): Format[T] = fmt

  def required(path:JsPath): Reads[JsValue] = Reads[JsValue] ( path.asSingleJsResult )

  def at[A](path:JsPath)(implicit reads:Reads[A]): Reads[A] =
    Reads[A]( js => path.asSingleJsResult(js).flatMap(reads.reads) )

  def optional[A](path:JsPath)(implicit reads:Reads[A]): Reads[Option[A]] = 
    Reads[Option[A]](json => path.asSingleJsResult(json).fold(_ => JsSuccess(None), a => reads.reads(a).map(Some(_))))

  def optional[A](implicit reads:Reads[A]):Reads[Option[A]] =
    Reads[Option[A]](js => JsSuccess(reads.reads(js).asOpt))

  def min(m:Int)(implicit reads:Reads[Int]) =
    filterNot[Int](ValidationError("validate.error.min"))(_ < m)(reads)

  def max(m:Int)(implicit reads:Reads[Int]) =
    filterNot[Int](ValidationError("validate.error.min"))(_ > m)(reads)

  def filterNot[A](error:ValidationError)(p:A => Boolean)(implicit reads:Reads[A]) = 
    Reads[A](js => reads.reads(js).filterNot(error)(p))

  def filter[A](otherwise: ValidationError)(p:A => Boolean)(implicit reads:Reads[A]) = 
    Reads[A](js => reads.reads(js).filter(otherwise)(p))

  def minLength[M](m:Int)(implicit reads:Reads[M], p: M => scala.collection.TraversableLike[_, M]) =
    filterNot[M](ValidationError(""))(_.size < m)

  def maxLength[M](m:Int)(implicit reads:Reads[M], p: M => scala.collection.TraversableLike[_, M]) =
    filterNot[M](ValidationError(""))(_.size > m)

  private val Email = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r
  def email(implicit reads:Reads[String]) : Reads[String] = Reads[String] { js =>
    reads.reads(js).collect(ValidationError("validate.error.email")) {
      case Email(e) => e
    }
  }
}

object ConstraintWrites extends ConstraintWrites

trait ConstraintWrites {
  def pruned[T](implicit w: Writes[T]): Writes[T] = new Writes[T] {
    def writes(t: T): JsValue = JsUndefined("pruned")
  }

  def at[A](path:JsPath)(implicit writes:Writes[A]): OWrites[A] =
    OWrites[A]{ a => JsPath.createObj(path -> writes.writes(a))
      /*Json.obj( "toto" -> writes.writes(a) )*/

      /*val js: JsObject = Json.obj()
      val obj: JsObject = path.set(js, js => writes.writes(a))
      obj*/
    }
}
