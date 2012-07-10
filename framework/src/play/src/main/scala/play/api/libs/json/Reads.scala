package play.api.libs.json

import scala.collection._
import Json._
import scala.annotation.implicitNotFound
import play.api.data.validation.ValidationError


case class JsSuccess[T](value: T) extends JsResult[T] {
  def get[T] = value
}

case class JsError[T](original: JsValue, errors: Seq[(JsPath, Seq[ValidationError])], globalErrors: Seq[ValidationError]) extends JsResult[T] {
  def get[T] = throw new NoSuchElementException("JsError[T].get")

  def toJson: JsValue = original // TODO
  def toJsonErrorsOnly: JsValue = original // TODO
  def toFlatForm: Seq[(String, Seq[ValidationError])] = errors.map{ case(path, seq) => path.toJsonString -> seq } :+ ("globals" -> globalErrors) // TODO
}

object JsError {
  def apply[T](original: JsValue, errors: (JsPath, Seq[ValidationError])*) = new JsError[T](original, errors, Seq())

  def merge(e1: Seq[(JsPath, Seq[ValidationError])], e2: Seq[(JsPath, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] = {
    import scala.collection.mutable.ListBuffer
    val lb = ListBuffer[(JsPath, Seq[ValidationError])]() ++ e2
    e1.map{ case(path, errors) => 
      val lr = lb.collect{ case elt if(elt._1 == path) => lb-=elt; elt._2 }
      path -> (errors ++ lr.flatten).distinct
    } ++ lb
  }
}

sealed trait JsResult[T] {
  def fold[X](valid: T => X, invalid: (JsValue, Seq[(JsPath, Seq[ValidationError])], Seq[ValidationError]) => X): X = this match {
    case JsSuccess(v) => valid(v)
    case JsError(o, e, g) => invalid(o, e, g)
  }

  def fold[X](valid: T => X, invalid: JsError[T] => X): X = this match {
    case JsSuccess(s) => valid(s)
    case e @ JsError(_, _, _) => invalid(e)
  }

  def map[X](f: T => X): JsResult[X] = this match {
    case JsSuccess(v) => JsSuccess(f(v))
    case JsError(o, e, g) => JsError[X](o, e, g)
  }

  def flatMap[X](f: T => JsResult[X]): JsResult[X] = this match {
    case JsSuccess(v) => f(v)
    case JsError(o, e, g) => JsError[X](o, e, g)
  }

  def flatMapTryDefault[X](defaultValue: T)(f: T => JsResult[X]): JsResult[X] = this match {
    case JsSuccess(v) => f(v)
    case JsError(o, e, g) => 
      // tries with undefined first
      f(defaultValue) match {
        case s @ JsSuccess(_) => s
        case JsError(o2, e2, g2) => JsError[X](o, 
          JsError.merge(e, e2),
          //(e ++ e2).distinct.groupBy{ case(path, _) => path }.map{ case(k, v) => k -> v.map(_._2).flatten }.toSeq,
          g ++ g2)
      }
  }

  def prod[V](other: JsResult[V]): JsResult[(T, V)] = {
    (this, other) match {
      case (JsSuccess(t), JsSuccess(v)) => JsSuccess((t, v))
      case (JsError(o, e, g), JsSuccess(v)) => JsError[(T, V)](o, e, g)
      case (JsSuccess(v), JsError(o, e, g)) => JsError[(T, V)](o, e, g)
      case (JsError(o, e, g), JsError(o2, e2, g2)) => JsError[(T, V)](o, 
          JsError.merge(e, e2),
          g ++ g2)
      case _ => throw new RuntimeException("JsValue.prod operator can't be applied on other ")
    }
  }

  def and(other: JsResult[T]): JsResult[T] = {
    (this, other) match {
      case (JsSuccess(t1), JsSuccess(t2)) => JsSuccess(t1)
      case (JsError(o, e, g), JsSuccess(v)) => JsError[T](o, e, g)
      case (JsSuccess(v), JsError(o, e, g)) => JsError[T](o, e, g)
      case (JsError(o, e, g), JsError(o2, e2, g2)) => JsError[T](o ++ o2, 
        JsError.merge(e, e2),
        g ++ g2)
      case _ => throw new RuntimeException("JsValue.prod operator can't be applied on other ")
    }
  }

  def andThen[V](other: JsResult[V]): JsResult[V] = {
    (this, other) match {
      case (JsSuccess(t), JsSuccess(v)) => JsSuccess(v)
      case (JsError(o, e, g), JsSuccess(v)) => JsError[V](o, e, g)
      case (JsSuccess(t), JsError(o, e, g)) => JsError[V](o, e, g)
      case (JsError(o, e, g), JsError(o2, e2, g2)) => JsError[V](o ++ o2, 
        JsError.merge(e, e2),
        g ++ g2)
      case _ => throw new RuntimeException("JsValue.prod operator can't be applied on other ")
    }
  }

  def or(other: JsResult[T]): JsResult[T] = {
    (this, other) match {
      case (JsSuccess(t1), JsSuccess(t2)) => JsSuccess(t1)
      case (JsError(o, e, g), JsSuccess(t)) => JsSuccess(t)
      case (JsSuccess(t), JsError(o, e, g)) => JsSuccess(t)
      case (JsError(o, e, g), JsError(o2, e2, g2)) => JsError[T](o ++ o2, 
        JsError.merge(e, e2),
        g ++ g2)
      case _ => throw new RuntimeException("JsValue.prod operator can't be applied on other ")
    }
  }

  def rebase(json: JsValue): JsResult[T] = fold(valid = JsSuccess(_), invalid = (_, e, g) => JsError(json, e, g))
  def repath(path: JsPath): JsResult[T] = fold(valid = JsSuccess(_), invalid = (o, e, g) => JsError(o, e.map{ case (p, s) => path ++ p -> s }, g))

  def get[T]

  def getOrElse[T](t: T) = this match {
    case JsSuccess(_) => get
    case JsError(_, _, _) => t
  }

  def asOpt[T] = this match {
    case JsSuccess(v) => Some(v)
    case JsError(_, _, _) => None
  }

  def asEither[T] = this match {
    case JsSuccess(v) => Right(v)
    case JsError(o, e, g) => Left((o, e, g))
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

  def and(other: Reads[T]) = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = self.reads(json) and other.reads(json)
  }

  def andThen[V](other: Reads[V]) = new Reads[V] {
    def reads(json: JsValue): JsResult[V] = self.reads(json) andThen other.reads(json)
  }

  def or(other: Reads[T]) = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = self.reads(json) or other.reads(json)
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
object Reads extends DefaultReads

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
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for Short types.
   */
  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toShort)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for Long types.
   */
  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toLong)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for Float types.
   */
  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toFloat)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for Double types.
   */
  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toDouble)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for BigDecimal types.
   */
  implicit object BigDecimalReads extends Reads[BigDecimal] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))
    }
  }

  /**
   * Deserializer for Boolean types.
   */
  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsboolean")))
    }
  }

  /**
   * Deserializer for String types.
   */
  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => JsSuccess(s)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsstring")))
    }
  }


  /**
   * Deserializer for JsObject.
   */
  implicit object JsObjectReads extends Reads[JsObject] {
    def reads(json: JsValue) = json match {
      case o: JsObject => JsSuccess(o)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsobject")))
    }
  }

  implicit object JsArrayReads extends Reads[JsArray] {
    def reads(json: JsValue) = json match {
      case o: JsArray => JsSuccess(o)
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsarray")))
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
    def reads(json: JsValue) = fmt.reads(json).fold( v => JsSuccess(Some(v)), (o, e, g) => JsSuccess(None) )
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
            case JsSuccess(v) => Right( (key, v, value) )
            case JsError(o, e, g) =>
              hasErrors = true
              Left( ( e.map{ case (p, valerr) => (JsPath \ key) ++ p -> valerr }, g) )
          } 
        }

        // if errors, tries to merge them into a single JsError
        if(hasErrors) {
          val (fulle, fullg) = r.filter( _.isLeft ).map( _.left.get )
                                .foldLeft(List[(JsPath, Seq[ValidationError])]() -> List[ValidationError]())( (acc, v) => (acc._1 ++ v._1, acc._2 ++ v._2) )
          JsError(json, fulle, fullg)
        }
        // no error, rebuilds the map
        else JsSuccess( r.filter( _.isRight ).map( _.right.get ).map{ v => v._1 -> v._2 }.toMap )
      }
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsobject")))
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
            case JsSuccess(v) => Right(v)
            case JsError(o, e, g) => 
              hasErrors = true
              Left( ( e.map{ case (p, valerr) => (JsPath(idx)) ++ p -> valerr }, g) )
          }
        }

        // if errors, tries to merge them into a single JsError
        if(hasErrors) {
          val (fulle, fullg) = r.filter( _.isLeft ).map( _.left.get )
                                .foldLeft(List[(JsPath, Seq[ValidationError])]() -> List[ValidationError]())( (acc, v) => (acc._1 ++ v._1, acc._2 ++ v._2) )          
          JsError(json, fulle, fullg)
        }
        // no error, rebuilds the map
        else {
          val builder = bf()
          r.foreach( builder += _.right.get )
          JsSuccess(builder.result())
        }

      }
      case _ => JsError(json, JsPath() -> Seq(ValidationError("validate.error.expected.jsarray")))
    }
  }

  /**
   * Deserializer for Array[T] types.
   */
  implicit def ArrayReads[T: Reads: Manifest]: Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json.validate[List[T]].map( _.toArray )
  }

}




