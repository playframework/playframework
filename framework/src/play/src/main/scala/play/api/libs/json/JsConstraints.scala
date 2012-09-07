package play.api.libs.json

import play.api.data.validation.ValidationError
import Json._

trait ConstraintFormat {
  def of[A](implicit fmt: Format[A]): Format[A] = fmt

  def optional[A](implicit fmt: Format[A]): Format[Option[A]] = Format[Option[A]]( Reads.optional(fmt), Writes.optional(fmt) )
}

trait PathFormat {
  def at[A](path: JsPath)(implicit f:Format[A]): OFormat[A] = 
    OFormat[A](Reads.at(path)(f), Writes.at(path)(f)) 

}

trait PathReads {
  
  def required(path:JsPath): Reads[JsValue] = at(path)

  def at[A](path:JsPath)(implicit reads:Reads[A]): Reads[A] =
    Reads[A]( js => path.asSingleJsResult(js).flatMap(reads.reads(_).repath(path)))

  def optional[A](path:JsPath)(implicit reads:Reads[A]): Reads[Option[A]] = 
    Reads[Option[A]](json => path.asSingleJsResult(json).fold(_ => JsSuccess(None), a => reads.reads(a).repath(path).map(Some(_))))

}

trait ConstraintReads {
  def optional[A](implicit reads:Reads[A]):Reads[Option[A]] =
    Reads[Option[A]](js => JsSuccess(reads.reads(js).asOpt))

  def list[A](implicit reads:Reads[A]): Reads[List[A]] = Reads.traversableReads[List, A]
  def set[A](implicit reads:Reads[A]): Reads[Set[A]] = Reads.traversableReads[Set, A]
  def seq[A](implicit reads:Reads[A]): Reads[Seq[A]] = Reads.traversableReads[Seq, A]
  def map[A](implicit reads:Reads[A]): Reads[collection.immutable.Map[String, A]] = Reads.mapReads[A]

  def min(m:Int)(implicit reads:Reads[Int]) =
    filterNot[Int](ValidationError("validate.error.min", m))(_ < m)(reads)

  def max(m:Int)(implicit reads:Reads[Int]) =
    filterNot[Int](ValidationError("validate.error.max", m))(_ > m)(reads)

  def filterNot[A](error:ValidationError)(p:A => Boolean)(implicit reads:Reads[A]) = 
    Reads[A](js => reads.reads(js).filterNot(error)(p))

  def filter[A](otherwise: ValidationError)(p:A => Boolean)(implicit reads:Reads[A]) = 
    Reads[A](js => reads.reads(js).filter(otherwise)(p))

  def minLength[M](m:Int)(implicit reads:Reads[M], p: M => scala.collection.TraversableLike[_, M]) =
    filterNot[M](ValidationError("validate.error.minlength", m))(_.size < m)

  def maxLength[M](m:Int)(implicit reads:Reads[M], p: M => scala.collection.TraversableLike[_, M]) =
    filterNot[M](ValidationError("validate.error.maxlength", m))(_.size > m)

  /**
   * Defines a regular expression constraint for `String` values, i.e. the string must match the regular expression pattern
   */
  def pattern(regex: => scala.util.matching.Regex, error: String = "error.pattern")(implicit reads:Reads[String]) = 
    Reads[String]( js => reads.reads(js).flatMap { o =>
      regex.unapplySeq(o).map( _ => JsSuccess(o) ).getOrElse(JsError(error))
    }) 

  def email(implicit reads:Reads[String]) : Reads[String] = 
    pattern( """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r, "validate.error.email" )

  def verifying[A](cond: A => Boolean)(implicit _reads: Reads[A]) =
    filter[A](ValidationError("validation.error.condition.not.verified"))(cond)(_reads)

  def verifyingIf[A](cond: A => Boolean)(subreads: Reads[_])(implicit _reads: Reads[A]) = 
    Reads[A] { js => _reads.reads(js).flatMap { t => 
      (scala.util.control.Exception.catching(classOf[MatchError]) opt cond(t)).flatMap { b =>
        if(b) Some(subreads.reads(js).map( _ => t ))
        else None
      }.getOrElse(JsSuccess(t))
    } }

  def pure[A](a: A) = new Reads[A] {
    def reads(json: JsValue) = JsSuccess(a)
  }
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

  def pure[A](path: JsPath, fixed: A)(implicit _writes:Writes[A]) =
    OWrites[A]{ a => JsPath.createObj(path -> _writes.writes(fixed)) }    
}

trait ConstraintWrites {
  def optional[A](implicit wa: Writes[A]): Writes[Option[A]] = Writes[Option[A]] { a => a match {
      case None => Json.obj()
      case Some(av) => wa.writes(av)
    }
  }

  def pruned[A](implicit w: Writes[A]): Writes[A] = new Writes[A] {
    def writes(a: A): JsValue = JsUndefined("pruned")
  }

}
