package play.api.libs.json

import scala.collection._
import Json._
import scala.annotation.implicitNotFound
import play.api.data.validation.ValidationError

/**
 * Json deserializer: write an implicit to define a deserializer for any type.
 */
@implicitNotFound(
  "No Json deserializer found for type ${A}. Try to implement an implicit Reads or Format for this type."
)
trait Reads[A] {
  self =>
  /**
   * Convert the JsValue into a A
   */
  def reads(json: JsValue): JsResult[A]

  def map[B](f:A => B):Reads[B] =
    Reads[B] { json => self.reads(json).map(f) }

  def flatMap[B](f:A => Reads[B]):Reads[B] = Reads[B] { json =>
    self.reads(json).flatMap(t => f(t).reads(json))
  }

  def collect[B](error:ValidationError)(f: PartialFunction[A,B]) =
    Reads[B] { json => self.reads(json).collect(error)(f) }

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

    def pure[A](a:A):Reads[A] = Reads[A] { _ => JsSuccess(a) }

    def map[A,B](m:Reads[A], f: A => B):Reads[B] = m.map(f)

    def apply[A,B](mf:Reads[A => B], ma: Reads[A]):Reads[B] = new Reads[B]{ def reads(js: JsValue) = applicativeJsResult(mf.reads(js),ma.reads(js)) }

  }

  implicit def alternativeReads(implicit a: Applicative[Reads]):Alternative[Reads] = new Alternative[Reads]{
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

  def apply[A](_reads: JsValue => JsResult[A]): Reads[A] = new Reads[A] {
    def reads(json: JsValue) = _reads(json)
  }

  implicit def functorReads(implicit a: Applicative[Reads]) = new Functor[Reads]{
    def fmap[A, B](reads: Reads[A], f: A => B): Reads[B] = a.map(reads, f)
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
