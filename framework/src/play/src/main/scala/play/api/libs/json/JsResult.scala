package play.api.libs.json

import Json._
import play.api.data.validation.ValidationError

case class JsSuccess[T](value: T, path: JsPath = JsPath()) extends JsResult[T] {
  def get:T = value
}

case class JsError(errors: Seq[(JsPath, Seq[ValidationError])]) extends JsResult[Nothing] {
  def get:Nothing = throw new NoSuchElementException("JsError.get")

  //def toJson: JsValue = original // TODO
  //def toJsonErrorsOnly: JsValue = original // TODO
  //def toFlatForm: Seq[(String, Seq[ValidationError])] = errors.map{ case(path, seq) => path.toJsonString -> seq } :+ ("globals" -> globalErrors) // TODO
}

object JsError {

  def apply(error:ValidationError):JsError = JsError(Seq(JsPath() -> Seq(error)))

  def merge(e1: Seq[(JsPath, Seq[ValidationError])], e2: Seq[(JsPath, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] = {
    (e1 ++ e2).groupBy(_._1).mapValues( _.map(_._2).flatten ).toList
  }
}

sealed trait JsResult[+T] {
  def fold[X](invalid: Seq[(JsPath, Seq[ValidationError])] => X, valid: T => X): X = this match {
    case JsSuccess(v,_) => valid(v)
    case JsError(e) => invalid(e)
  }

  def map[X](f: T => X): JsResult[X] = this match {
    case JsSuccess(v,_) => JsSuccess(f(v))
    case JsError(e) => JsError(e)
  }

  def filterNot(error:ValidationError)(p: T => Boolean) =
    this.flatMap { a => if(p(a)) JsError(error) else JsSuccess(a) }

  def filter(otherwise:ValidationError)(p: T => Boolean) =
    this.flatMap { a => if(p(a)) JsSuccess(a) else JsError(otherwise) }

  def collect[B](otherwise:ValidationError)(p:PartialFunction[T,B]): JsResult[B] = flatMap {
    case t if p.isDefinedAt(t) => JsSuccess(p(t))
    case _ => JsError(otherwise)
  }

  def flatMap[X](f: T => JsResult[X]): JsResult[X] = this match {
    case JsSuccess(v, path) => f(v).repath(path)
    case JsError(e) => JsError(e)
  }

  //def rebase(json: JsValue): JsResult[T] = fold(valid = JsSuccess(_), invalid = (_, e, g) => JsError(json, e, g))
  def repath(path: JsPath): JsResult[T] = this match {
    case JsSuccess(a,p) => JsSuccess(a,path ++ p)
    case JsError(es) => JsError(es.map{ case (p, s) => path ++ p -> s })
  }

  def get:T

  def getOrElse[TT >: T](t: => TT):TT = this match {
    case JsSuccess(a,_) => a 
    case JsError(_) => t
  }

  def asOpt = this match {
    case JsSuccess(v,_) => Some(v)
    case JsError(_) => None
  }

  def asEither = this match {
    case JsSuccess(v,_) => Right(v)
    case JsError(e) => Left(e)
  }  
}

object JsResult {

  import play.api.libs.json.util._

  implicit def alternativeJsResult(implicit a:Applicative[JsResult]):Alternative[JsResult] = new Alternative[JsResult]{
    val app = a
    def |[A,B >: A](alt1: JsResult[A], alt2 :JsResult[B]):JsResult[B] = (alt1, alt2) match {
      case (JsError(e), JsSuccess(t,p)) => JsSuccess(t,p)
      case (JsSuccess(t,p), _) => JsSuccess(t,p)
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
    }
    def empty:JsResult[Nothing] = JsError(Seq())   
  }

  implicit val applicativeJsResult:Applicative[JsResult] = new Applicative[JsResult] {

    def pure[A](a:A):JsResult[A] = JsSuccess(a)

    def map[A,B](m:JsResult[A], f: A => B):JsResult[B] = m.map(f)

    def apply[A,B](mf:JsResult[A => B], ma: JsResult[A]):JsResult[B] = (mf, ma) match {
      case (JsSuccess(f,_), JsSuccess(a,_)) => JsSuccess(f(a))
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
      case (JsError(e), _) => JsError(e)
      case (_, JsError(e)) => JsError(e)
    }
  }
}