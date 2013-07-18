package play.api.libs.json

import Json._
import play.api.data.validation.ValidationError

case class JsSuccess[T](value: T, path: JsPath = JsPath()) extends JsResult[T] {
  def get: T = value
}

case class JsError(errors: Seq[(JsPath, Seq[ValidationError])]) extends JsResult[Nothing] {
  def get: Nothing = throw new NoSuchElementException("JsError.get")

  def ++(error: JsError): JsError = JsError.merge(this, error)

  def :+(error: (JsPath, ValidationError)): JsError = JsError.merge(this, JsError(error))
  def append(error: (JsPath, ValidationError)): JsError = this.:+(error)

  def +:(error: (JsPath, ValidationError)): JsError = JsError.merge(JsError(error), this)
  def prepend(error: (JsPath, ValidationError)): JsError = this.+:(error)

}

object JsError {

  def apply(): JsError = JsError(Seq(JsPath() -> Seq()))
  def apply(error: ValidationError): JsError = JsError(Seq(JsPath() -> Seq(error)))
  def apply(error: String): JsError = JsError(ValidationError(error))
  def apply(error: (JsPath, ValidationError)): JsError = JsError(Seq(error._1 -> Seq(error._2)))
  def apply(path: JsPath, error: ValidationError): JsError = JsError(path -> error)
  def apply(path: JsPath, error: String): JsError = JsError(path -> ValidationError(error))

  def merge(e1: Seq[(JsPath, Seq[ValidationError])], e2: Seq[(JsPath, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] = {
    (e1 ++ e2).groupBy(_._1).mapValues(_.map(_._2).flatten).toList
  }

  def merge(e1: JsError, e2: JsError): JsError = {
    JsError(merge(e1.errors, e2.errors))
  }

  //def toJson: JsValue = original // TODO
  //def toJsonErrorsOnly: JsValue = original // TODO
  def toFlatForm(e: JsError): Seq[(String, Seq[ValidationError])] = e.errors.map { case (path, seq) => path.toJsonString -> seq }
  def toFlatJson(e: JsError): JsObject = toFlatJson(e.errors)
  def toFlatJson(errors: Seq[(JsPath, Seq[ValidationError])]): JsObject =
    errors.foldLeft(Json.obj()) { (obj, error) =>
      obj ++ Json.obj(error._1.toJsonString -> error._2.foldLeft(Json.arr()) { (arr, err) =>
        arr :+ Json.obj(
          "msg" -> err.message,
          "args" -> err.args.foldLeft(Json.arr()) { (arr, arg) =>
            arr :+ (arg match {
              case s: String => JsString(s)
              case nb: Int => JsNumber(nb)
              case nb: Short => JsNumber(nb)
              case nb: Long => JsNumber(nb)
              case nb: Double => JsNumber(nb)
              case nb: Float => JsNumber(nb)
              case b: Boolean => JsBoolean(b)
              case js: JsValue => js
              case x => JsString(x.toString)
            })
          }
        )
      })
    }
}

sealed trait JsResult[+A] { self =>

  def fold[X](invalid: Seq[(JsPath, Seq[ValidationError])] => X, valid: A => X): X = this match {
    case JsSuccess(v, _) => valid(v)
    case JsError(e) => invalid(e)
  }

  def map[X](f: A => X): JsResult[X] = this match {
    case JsSuccess(v, path) => JsSuccess(f(v), path)
    case e: JsError => e
  }

  def filterNot(error: ValidationError)(p: A => Boolean): JsResult[A] =
    this.flatMap { a => if (p(a)) JsError(error) else JsSuccess(a) }

  def filterNot(p: A => Boolean): JsResult[A] =
    this.flatMap { a => if (p(a)) JsError() else JsSuccess(a) }

  def filter(p: A => Boolean): JsResult[A] =
    this.flatMap { a => if (p(a)) JsSuccess(a) else JsError() }

  def filter(otherwise: ValidationError)(p: A => Boolean): JsResult[A] =
    this.flatMap { a => if (p(a)) JsSuccess(a) else JsError(otherwise) }

  def collect[B](otherwise: ValidationError)(p: PartialFunction[A, B]): JsResult[B] = flatMap {
    case t if p.isDefinedAt(t) => JsSuccess(p(t))
    case _ => JsError(otherwise)
  }

  def flatMap[X](f: A => JsResult[X]): JsResult[X] = this match {
    case JsSuccess(v, path) => f(v).repath(path)
    case e: JsError => e
  }

  def foreach(f: A => Unit): Unit = this match {
    case JsSuccess(a, _) => f(a)
    case _ => ()
  }

  def withFilter(p: A => Boolean) = new WithFilter(p)

  final class WithFilter(p: A => Boolean) {
    def map[B](f: A => B): JsResult[B] = self match {
      case JsSuccess(a, path) =>
        if (p(a)) JsSuccess(f(a), path)
        else JsError()
      case e: JsError => e
    }
    def flatMap[B](f: A => JsResult[B]): JsResult[B] = self match {
      case JsSuccess(a, path) =>
        if (p(a)) f(a).repath(path)
        else JsError()
      case e: JsError => e
    }
    def foreach(f: A => Unit): Unit = self match {
      case JsSuccess(a, _) if p(a) => f(a)
      case _ => ()
    }
    def withFilter(q: A => Boolean) = new WithFilter(a => p(a) && q(a))
  }

  //def rebase(json: JsValue): JsResult[A] = fold(valid = JsSuccess(_), invalid = (_, e, g) => JsError(json, e, g))
  def repath(path: JsPath): JsResult[A] = this match {
    case JsSuccess(a, p) => JsSuccess(a, path ++ p)
    case JsError(es) => JsError(es.map { case (p, s) => path ++ p -> s })
  }

  def get: A

  def getOrElse[AA >: A](t: => AA): AA = this match {
    case JsSuccess(a, _) => a
    case JsError(_) => t
  }

  def orElse[AA >: A](t: => JsResult[AA]): JsResult[AA] = this match {
    case s @ JsSuccess(_, _) => s
    case JsError(_) => t
  }

  def asOpt = this match {
    case JsSuccess(v, _) => Some(v)
    case JsError(_) => None
  }

  def asEither = this match {
    case JsSuccess(v, _) => Right(v)
    case JsError(e) => Left(e)
  }

  def recover[AA >: A](errManager: PartialFunction[JsError, AA]): JsResult[AA] = this match {
    case JsSuccess(v, p) => JsSuccess(v, p)
    case e: JsError => if (errManager isDefinedAt e) JsSuccess(errManager(e)) else this
  }

  def recoverTotal[AA >: A](errManager: JsError => AA): AA = this match {
    case JsSuccess(v, p) => v
    case e: JsError => errManager(e)
  }
}

object JsResult {

  import play.api.libs.functional._

  implicit def alternativeJsResult(implicit a: Applicative[JsResult]): Alternative[JsResult] = new Alternative[JsResult] {
    val app = a
    def |[A, B >: A](alt1: JsResult[A], alt2: JsResult[B]): JsResult[B] = (alt1, alt2) match {
      case (JsError(e), JsSuccess(t, p)) => JsSuccess(t, p)
      case (JsSuccess(t, p), _) => JsSuccess(t, p)
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
    }
    def empty: JsResult[Nothing] = JsError(Seq())
  }

  implicit val applicativeJsResult: Applicative[JsResult] = new Applicative[JsResult] {

    def pure[A](a: A): JsResult[A] = JsSuccess(a)

    def map[A, B](m: JsResult[A], f: A => B): JsResult[B] = m.map(f)

    def apply[A, B](mf: JsResult[A => B], ma: JsResult[A]): JsResult[B] = (mf, ma) match {
      case (JsSuccess(f, _), JsSuccess(a, _)) => JsSuccess(f(a))
      case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
      case (JsError(e), _) => JsError(e)
      case (_, JsError(e)) => JsError(e)
    }
  }
}
