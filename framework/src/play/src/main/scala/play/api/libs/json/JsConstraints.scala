package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

//object Constraints extends ConstraintReads with ConstraintWrites

object Constraints extends ConstraintFormat with ConstraintReads with ConstraintWrites

object PathFormat extends PathFormat

trait ConstraintFormat {
    def of[A](implicit fmt: Format[A]): Format[A] = fmt
}

trait PathFormat {
  def at[A](path: JsPath)(implicit r: Reads[A], w: Writes[A]) = 
    OFormat[A](PathReads.at(path)(r), PathWrites.at(path)(w)) 

}

object PathReads extends PathReads

trait PathReads {
  
  //def required(path:JsPath): Reads[JsValue] = Reads[JsValue] ( path.asSingleJsResult )
  def required[A](path:JsPath): Reads[JsValue] = at(path)

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

  def verifying[T](cond: T => Boolean)(implicit _reads: Reads[T]) = new Reads[T] {
    def reads(json: JsValue) = 
      _reads
      .reads(json)
      .filter(ValidationError("validation.error.condition.not.verified"))(cond)
  }

  def verifyingIf[T](cond: T => Boolean)(subreads: Reads[_])(implicit _reads: Reads[T]) = new Reads[T] {
    def reads(json: JsValue) = _reads.reads(json).flatMap { t => 
      (scala.util.control.Exception.catching(classOf[MatchError]) opt cond(t)).flatMap { b =>
        if(b) Some(subreads.reads(json).map( _ => t ))
        else None
      }.getOrElse(JsSuccess(t))
    }
  }
}

object PathWrites extends PathWrites

trait PathWrites {
  def at[A](path: JsPath)(implicit _writes:Writes[A]): OWrites[A] =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(a)) }

  def optional[A](path: JsPath)(implicit _writes:Writes[Option[A]]): OWrites[Option[A]] =
    OWrites[Option[A]]{ a => JsPath.createObj(path -> _writes.writes(a)) }  

  //def copy(path: JsPath): OWrites[JsObject] =
  //  OWrites[JsObject]{ obj => val ret = JsPath.createObj(path -> path(obj).headOption.getOrElse(JsNull)); println("COPY: path:%s ret:%s".format(path, ret)); ret }  

  /*def copy(path: JsPath)(implicit _writes: Writes[JsValue]): OWrites[JsObject] =
    OWrites[JsObject]{ obj => val ret = JsPath.createObj(path -> path(obj).headOption.map( _writes.writes ).getOrElse(JsNull)); println("COPY: path:%s ret:%s".format(path, ret)); ret }  
  */

  def copyJson(path: JsPath): OWrites[JsValue] =
    OWrites[JsValue]{ obj => val ret = JsPath.createObj(path -> path(obj).headOption.getOrElse(JsNull)); println("COPY: path:%s ret:%s".format(path, ret)); ret }  

  def buildJson(path: JsPath)(implicit _writes: OWrites[JsValue]): OWrites[JsValue] =
    OWrites[JsValue]{ obj => val ret = JsPath.createObj(path -> path(obj).headOption.map( _writes.writes ).getOrElse(JsNull)); println("COPY: path:%s ret:%s".format(path, ret)); ret }  

  def transformJson(path: JsPath, f: JsValue => JsValue): OWrites[JsValue] =
    OWrites[JsValue]{ obj => val ret = JsPath.createObj(path -> f(path(obj).headOption.getOrElse(JsNull)));println("TRANSFORM: path:%s ret:%s".format(path, ret)); ret }  

  def fixedValue[A](path: JsPath, a: A)(implicit _writes:Writes[A]) =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(a)) }    
}
trait ConstraintWrites {

  def pruned[T](implicit w: Writes[T]): Writes[T] = new Writes[T] {
    def writes(t: T): JsValue = JsUndefined("pruned")
  }

}
