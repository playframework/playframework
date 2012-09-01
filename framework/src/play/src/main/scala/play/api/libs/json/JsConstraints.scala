package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

//object Constraints extends ConstraintReads with ConstraintWrites

trait ConstraintFormat {
  def of[A](implicit fmt: Format[A]): Format[A] = fmt

  /**
   * Date Format
   *  - can read date as Int or Iso format _yyyy-MM-dd'T'HH:mm:ssz_
   *  - write date as Int
   */

}

trait PathFormat {
  def at[A](path: JsPath)(implicit f:Format[A]): OFormat[A] = 
    OFormat[A](Reads.at(path)(f), Writes.at(path)(f)) 

}

trait PathReads {
  
  def required(path:JsPath): Reads[JsValue] = at(path)

  def at[A](path:JsPath)(implicit reads:Reads[A]): Reads[A] =
    Reads[A]( js => path.asSingleJsResult(js).flatMap(reads.reads) )

  def optional[A](path:JsPath)(implicit reads:Reads[A]): Reads[Option[A]] = 
    Reads[Option[A]](json => path.asSingleJsResult(json).fold(_ => JsSuccess(None), a => reads.reads(a).map(Some(_))))

}

trait ConstraintReads {
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

  def verifying[A](cond: A => Boolean)(implicit _reads: Reads[A]) =
    filter[A](ValidationError("validation.error.condition.not.verified"))(cond)(_reads)

}

trait PathWrites {
  def at[A](path: JsPath)(implicit _writes:Writes[A]): OWrites[A] =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(a)) }

  def optional[A](path: JsPath)(implicit _writes:Writes[Option[A]]): OWrites[Option[A]] =
    at[Option[A]](path)

  def pick(path: JsPath): OWrites[JsValue] =
    OWrites[JsValue]{ obj => JsPath.createObj(path -> path(obj).headOption.getOrElse(JsNull)) }

  def modifyJson(path: JsPath)(_writes: Writes[JsValue]): OWrites[JsValue] =
    OWrites[JsValue]{ obj => obj match {
      // if JsObject, try to aggregate
      case theObj @ JsObject(_) => theObj.deepMerge(JsPath.createObj(path -> path(obj).headOption.map( _writes.writes ).getOrElse(JsNull)))
      case _ => JsPath.createObj(path -> path(obj).headOption.map( _writes.writes ).getOrElse(JsNull))
    }
  }

  def transformJson(path: JsPath, f: JsValue => JsValue): OWrites[JsValue] =
    OWrites[JsValue]{ obj => JsPath.createObj(path -> f(path(obj).headOption.getOrElse(JsNull))) }  

  def fixedValue[A](path: JsPath, fixed: A)(implicit _writes:Writes[A]) =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(fixed)) }    
}

trait ConstraintWrites {

  def pruned[T](implicit w: Writes[T]): Writes[T] = new Writes[T] {
    def writes(t: T): JsValue = JsUndefined("pruned")
  }

}
