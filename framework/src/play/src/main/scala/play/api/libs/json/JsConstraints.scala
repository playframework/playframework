package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

//object Constraints extends ConstraintReads with ConstraintWrites

object Constraints extends ConstraintFormat with ConstraintReads with ConstraintWrites

object PathFormat extends PathFormat

trait ConstraintFormat {
  def of[A](implicit fmt: Format[A]): Format[A] = fmt

  /**
   * Date Format
   *  - can read date as Int or Iso format _yyyy-MM-dd'T'HH:mm:ssz_
   *  - write date as Int
   */
  implicit object dateFormat extends Format[java.util.Date] {    
    def reads(json: JsValue): JsResult[java.util.Date] = json match {
      case JsNumber(d) => JsSuccess(new java.util.Date(d.toInt))
      case JsString(s) => parseDate(s) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date.isoformat"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }

    def writes(d: java.util.Date) = JsNumber(d.getTime)


    private def parseDate(input: String): Option[java.util.Date] = {
      //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
      //things a bit.  Before we go on we have to repair this.
      val df = new java.text.SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" )
      
      //this is zero time so we need to add that TZ indicator for 
      val inputStr = if ( input.endsWith( "Z" ) ) {
          input.substring( 0, input.length() - 1) + "GMT-00:00"
        } else {
            val inset = 6
        
            val s0 = input.substring( 0, input.length - inset )
            val s1 = input.substring( input.length - inset, input.length )

            s0 + "GMT" + s1
        }
      
      try { Some(df.parse( input )) } catch {
        case _: java.text.ParseException => None
      }
      
    }
  }

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

  def verifying[A](cond: A => Boolean)(implicit _reads: Reads[A]) =
    filter[A](ValidationError("validation.error.condition.not.verified"))(cond)(_reads)

}

object PathWrites extends PathWrites

trait PathWrites {
  def at[A](path: JsPath)(implicit _writes:Writes[A]): OWrites[A] =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(a)) }

  def optional[A](path: JsPath)(implicit _writes:Writes[Option[A]]): OWrites[Option[A]] =
    at[Option[A]](path)

  def pick(path: JsPath): OWrites[JsValue] =
    OWrites[JsValue]{ obj => JsPath.createObj(path -> path(obj).headOption.getOrElse(JsNull)) }

  def createJson(path: JsPath)(_writes: OWrites[JsValue]): OWrites[JsValue] =
    at[JsValue](path)(_writes)

  def modifyJson(path: JsPath)(_writes: OWrites[JsValue]): OWrites[JsValue] =
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
