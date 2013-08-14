package play.api.libs.json

object Mappings {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._
  import play.api.data.validation._
  import play.api.data.validation.Mappings._

  import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode, _ }
  private def pathToJsPath(p: Path[JsValue]) =
    play.api.libs.json.JsPath(p.path.map{
      case KeyPathNode(key) => JSKeyPathNode(key)
      case IdxPathNode(i) => JIdxPathNode(i)
    })

  implicit def jsonAsString = Mapping[ValidationError, JsValue, String] {
    case JsString(v) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "String")))
  }

  implicit def jsonAsBoolean = Mapping[ValidationError, JsValue, Boolean] {
    case JsBoolean(v) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Boolean")))
  }

  // Note: Mappings of JsNumber to Number are validating that the JsNumber is indeed valid
  // in the target type. i.e: JsNumber(4.5) is not considered parseable as an Int.
  // That's a bit stricter than the "old" Read, which just cast to the target type, possibly loosing data.
  implicit def jsonAsInt = Mapping[ValidationError, JsValue, Int] {
    case JsNumber(v) if v.isValidInt => Success(v.toInt)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Int")))
  }

  implicit def jsonAsShort = Mapping[ValidationError, JsValue, Short] {
    case JsNumber(v) if v.isValidShort => Success(v.toShort)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Short")))
  }

  implicit def jsonAsLong = Mapping[ValidationError, JsValue, Long] {
    case JsNumber(v) if v.isValidLong => Success(v.toLong)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Long")))
  }

  implicit def jsonAsJsNumber = Mapping[ValidationError, JsValue, JsNumber] {
    case v@JsNumber(_) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "JsNumber")))
  }

  implicit def jsonAsJsBoolean = Mapping[ValidationError, JsValue, JsBoolean] {
    case v@JsBoolean(_) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "JsBoolean")))
  }

  implicit def jsonAsJsString = Mapping[ValidationError, JsValue, JsString] {
    case v@JsString(_) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "JsString")))
  }

  implicit def jsonAsJsObject = Mapping[ValidationError, JsValue, JsObject] {
    case v@JsObject(_) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "JsObject")))
  }

  // BigDecimal.isValidFloat is buggy, see [SI-6699]
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(java.lang.Float.toString(d), bd.mc)) == 0
  }
  implicit def jsonAsFloat = Mapping[ValidationError, JsValue, Float] {
    case JsNumber(v) if isValidFloat(v) => Success(v.toFloat)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Float")))
  }

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(java.lang.Double.toString(d), bd.mc)) == 0
  }
  implicit def jsonADouble = Mapping[ValidationError, JsValue, Double] {
    case JsNumber(v) if isValidDouble(v) => Success(v.toDouble)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Double")))
  }

  implicit def jsonAsBigDecimal = Mapping[ValidationError, JsValue, BigDecimal] {
    case JsNumber(v) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "BigDecimal")))
  }

  import java.math.{ BigDecimal => jBigDecimal }
  implicit def jsonAsJavaBigDecimal = Mapping[ValidationError, JsValue, jBigDecimal] {
    case JsNumber(v) => Success(v.bigDecimal)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "java.math.BigDecimal")))
  }

  implicit def jsonAsArray[O](implicit m: Mapping[ValidationError, JsValue, O], c: scala.reflect.ClassTag[O]) = Mapping[ValidationError, JsValue, Array[O]] {
    jsonAsSeq(m)(_).map(_.toArray)
  }

  implicit def jsonAsTraversable[O](implicit m: Mapping[ValidationError, JsValue, O]) = Mapping[ValidationError, JsValue, Traversable[O]] {
    jsonAsSeq(m)(_).map(_.toTraversable)
  }

  implicit def jsonAsSeq[O](implicit m: Mapping[ValidationError, JsValue, O]): Mapping[ValidationError, JsValue, Seq[O]] = Mapping[ValidationError, JsValue, Seq[O]] {
    case JsArray(vs) => Validation.sequence(vs.map(m))
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Array")))
  }

  implicit def pickInJson[O](p: Path[JsValue])(implicit m: Mapping[ValidationError, JsValue, O]) = Mapping[ValidationError, JsValue, O] { json =>
    val v: Validation[ValidationError, JsValue] = pathToJsPath(p)(json) match {
      case Nil => Failure(Seq(ValidationError("validation.required")))
      case js :: _ => Success(js)
    }
    v.flatMap(m)
  }

}