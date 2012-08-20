package play.api.libs.json

import scala.collection._
import Json._
import scala.annotation.implicitNotFound
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

/**
 * Json deserializer: write an implicit to define a deserializer for any type.
 */
@implicitNotFound(
  "No Json deserializer found for type ${T}. Try to implement an implicit Reads or Format for this type."
)
trait Reads[T] {
  self =>
  /**
   * Convert the JsValue into a T
   */
  def reads(json: JsValue): JsResult[T]
/*
  def and(other: Reads[T]) = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = self.reads(json) and other.reads(json)
  }

  def andThen[V](other: Reads[V]) = new Reads[V] {
    def reads(json: JsValue): JsResult[V] = self.reads(json) andThen other.reads(json)
  }

  def or(other: Reads[T]) = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = self.reads(json) or other.reads(json)
  }
*/
  def map[B](f:T => B):Reads[B] = new Reads[B] {
    def reads(json: JsValue): JsResult[B] = self.reads(json).map(f)
  }

  def flatMap[B](f:T => Reads[B]):Reads[B] = new Reads[B] {
    def reads(json: JsValue): JsResult[B] = self.reads(json).flatMap(t => f(t).reads(json))

  }


  /**
   * builds a JsErrorObj JsObject
   * {
   *    __VAL__ : "current known erroneous jsvalue",
   *    __ERR__ : "the i18n key of the error msg",
   *    __ARGS__ : "the args for the error msg" (JsArray)
   * } 
   */
  def JsErrorObj(knownValue: JsValue, key: String, args: JsValue*) = Reads.JsErrorObj(knownValue, key, args: _*)
}

/**
 * Default deserializer type classes.
 */
object Reads extends DefaultReads {

  import play.api.libs.json.util._

  implicit def applicativeReads(implicit applicativeJsResult:Applicative[JsResult]):Applicative[Reads] = new Applicative[Reads]{

    def pure[A](a:A):Reads[A] = new Reads[A] { def reads(js: JsValue) = JsSuccess(a) }

    def map[A,B](m:Reads[A], f: A => B):Reads[B] = m.map(f)

    def apply[A,B](mf:Reads[A => B], ma: Reads[A]):Reads[B] = new Reads[B]{ def reads(js: JsValue) = applicativeJsResult(mf.reads(js),ma.reads(js)) }

  }

  implicit def alternativeReads(implicit a:Applicative[Reads]):Alternative[Reads] = new Alternative[Reads]{
    val app = a
    def |[A,B >: A](alt1: Reads[A], alt2 :Reads[B]):Reads[B] = new Reads[B] {
      def reads(js: JsValue) = alt1.reads(js) match {
        case r@JsSuccess(_,_) => r
        case r@JsError(es1) => alt2.reads(js) match {
          case r2@JsSuccess(_,_) => r2
          case r2@JsError(es2) => JsError(JsError.merge(es1,es2))
        }
      }
    }
    def empty:Reads[Nothing] = new Reads[Nothing] { def reads(js: JsValue) = JsError(Seq()) }
  }

  def apply[A](reads: JsValue => JsResult[A]): Reads[A] = new Reads[A] {
    def reads(json: JsValue) = reads(json)
  }

}

/**
 * Default deserializer type classes.
 */
trait DefaultReads {

  /**
   * builds a JsErrorObj JsObject
   * {
   *    __VAL__ : "current known erroneous jsvalue",
   *    __ERR__ : "the i18n key of the error msg",
   *    __ARGS__ : "the args for the error msg" (JsArray)
   * } 
   */
  def JsErrorObj(knownValue: JsValue, key: String, args: JsValue*) = {
    Json.obj(
      "__VAL__" -> knownValue,
      "__ERR__" -> key,
      "__ARGS__" -> args.foldLeft(JsArray())( (acc: JsArray, arg: JsValue) => acc :+ arg )
    )
  }

  /**
   * Deserializer for Int types.
   */
  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toInt)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for Short types.
   */
  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toShort)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for Long types.
   */
  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toLong)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for Float types.
   */
  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toFloat)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for Double types.
   */
  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toDouble)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for BigDecimal types.
   */
  implicit object BigDecimalReads extends Reads[BigDecimal] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  /**
   * Deserializer for Boolean types.
   */
  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsboolean"))))
    }
  }

  /**
   * Deserializer for String types.
   */
  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => JsSuccess(s)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
  }


  /**
   * Deserializer for JsObject.
   */
  implicit object JsObjectReads extends Reads[JsObject] {
    def reads(json: JsValue) = json match {
      case o: JsObject => JsSuccess(o)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsobject"))))
    }
  }

  implicit object JsArrayReads extends Reads[JsArray] {
    def reads(json: JsValue) = json match {
      case o: JsArray => JsSuccess(o)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsarray"))))
    }
  }


  /**
   * Deserializer for JsValue.
   */
  implicit object JsValueReads extends Reads[JsValue] {
    def reads(json: JsValue) = JsSuccess(json)
  }

  implicit def OptionReads[T](implicit fmt: Reads[T]): Reads[Option[T]] = new Reads[Option[T]] {
    import scala.util.control.Exception._
    def reads(json: JsValue) = fmt.reads(json).fold( e => JsSuccess(None), v => JsSuccess(Some(v)))
  }

  /**
   * Deserializer for Map[String,V] types.
   */
  implicit def mapReads[V](implicit fmtv: Reads[V]): Reads[collection.immutable.Map[String, V]] = new Reads[collection.immutable.Map[String, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => {
        // first validates prod separates JsError / JsResult in an Seq[Either( (key, errors, globals), (key, v, jselt) )]
        // the aim is to find all errors prod then to merge them all
        var hasErrors = false

        val r = m.map { case (key, value) => 
          fromJson[V](value)(fmtv) match {
            case JsSuccess(v,_) => Right( (key, v, value) )
            case JsError(e) =>
              hasErrors = true
              Left( e.map{ case (p, valerr) => (JsPath \ key) ++ p -> valerr } )
          } 
        }

        // if errors, tries to merge them into a single JsError
        if(hasErrors) {
          val fulle = r.filter( _.isLeft ).map( _.left.get )
                                .foldLeft(List[(JsPath, Seq[ValidationError])]())( (acc, v) => acc ++ v )
          JsError(fulle)
        }
        // no error, rebuilds the map
        else JsSuccess( r.filter( _.isRight ).map( _.right.get ).map{ v => v._1 -> v._2 }.toMap )
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsobject"))))
    }
  }

  /**
   * Generic deserializer for collections types.
   */
  implicit def traversableReads[F[_], A](implicit bf: generic.CanBuildFrom[F[_], A, F[A]], ra: Reads[A]) = new Reads[F[A]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => {
        
        var hasErrors = false

        // first validates prod separates JsError / JsResult in an Seq[Either]
        // the aim is to find all errors prod then to merge them all
        val r = ts.zipWithIndex.map { case (elt, idx) => fromJson[A](elt)(ra) match {
            case JsSuccess(v,_) => Right(v)
            case JsError(e) => 
              hasErrors = true
              Left( e.map{ case (p, valerr) => (JsPath(idx)) ++ p -> valerr } )
          }
        }

        // if errors, tries to merge them into a single JsError
        if(hasErrors) {
          val fulle = r.filter( _.isLeft ).map( _.left.get )
                                .foldLeft(List[(JsPath, Seq[ValidationError])]())( (acc, v) => (acc ++ v) )          
          JsError(fulle)
        }
        // no error, rebuilds the map
        else {
          val builder = bf()
          r.foreach( builder += _.right.get )
          JsSuccess(builder.result())
        }

      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsarray"))))
    }
  }

  /**
   * Deserializer for Array[T] types.
   */
  implicit def ArrayReads[T: Reads: Manifest]: Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json.validate[List[T]].map( _.toArray )
  }

}
