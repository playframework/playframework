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

  private def jsonAs[T](f: PartialFunction[JsValue, Validation[ValidationError, T]])(args: Any*) =
    Rule.fromMapping[JsValue, T](
      f.orElse{ case j => Failure(Seq(ValidationError("validation.type-mismatch", args: _*)))
    })

  implicit def jsonAsString = jsonAs[String] {
    case JsString(v) => Success(v)
  }("String")

  implicit def jsonAsBoolean = jsonAs[Boolean]{
    case JsBoolean(v) => Success(v)
  }("Boolean")

  // Note: Mappings of JsNumber to Number are validating that the JsNumber is indeed valid
  // in the target type. i.e: JsNumber(4.5) is not considered parseable as an Int.
  // That's a bit stricter than the "old" Read, which just cast to the target type, possibly loosing data.
  implicit def jsonAsInt = jsonAs[Int]{
    case JsNumber(v) if v.isValidInt => Success(v.toInt)
  }("Int")

  implicit def jsonAsShort = jsonAs[Short]{
    case JsNumber(v) if v.isValidShort => Success(v.toShort)
  }("Short")

  implicit def jsonAsLong = jsonAs[Long]{
    case JsNumber(v) if v.isValidLong => Success(v.toLong)
  }("Long")

  implicit def jsonAsJsNumber = jsonAs[JsNumber]{
    case v@JsNumber(_) => Success(v)
  }("Number")

  implicit def jsonAsJsBoolean = jsonAs[JsBoolean]{
    case v@JsBoolean(_) => Success(v)
  }("Boolean")

  implicit def jsonAsJsString = jsonAs[JsString] {
    case v@JsString(_) => Success(v)
  }("String")

  implicit def jsonAsJsObject = jsonAs[JsObject] {
    case v@JsObject(_) => Success(v)
  }("Object")

  implicit def jsonAsJsArray = jsonAs[JsArray] {
    case v@JsArray(_) => Success(v)
  }("Array")

  // BigDecimal.isValidFloat is buggy, see [SI-6699]
  import java.{lang => jl}
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Float.toString(d), bd.mc)) == 0
  }
  implicit def jsonAsFloat = jsonAs[Float] {
    case JsNumber(v) if isValidFloat(v) => Success(v.toFloat)
  }("Float")

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Double.toString(d), bd.mc)) == 0
  }
  implicit def jsonADouble =jsonAs[Double] {
    case JsNumber(v) if isValidDouble(v) => Success(v.toDouble)
  }("Double")

  implicit def jsonAsBigDecimal = jsonAs[BigDecimal] {
    case JsNumber(v) => Success(v)
  }("BigDecimal")

  import java.{ math => jm }
  implicit def jsonAsJavaBigDecimal = jsonAs[jm.BigDecimal] {
    case JsNumber(v) => Success(v.bigDecimal)
  }("BigDecimal")

  /*
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

  // TODO: should key exact path of the error(s)
  // It seems that this mapping should not exist.
  // instead, we should have a rule.
  implicit def jsonAsMap[O](implicit m: Mapping[ValidationError, JsValue, O]) = Mapping[ValidationError, JsValue, Map[String, O]] {
    jsonAsJsObject(_).flatMap { case JsObject(fields) =>
      Validation.sequence(fields.map(f => m(f._2)))
        .map { os => fields.map(_._1).zip(os).toMap }
    }
  }
  */

  implicit def jsonAsSeq[O](implicit r: Rule[JsValue, O]): Rule[JsValue, Seq[O]] =
    jsonAsJsArray // XXX: jsonAsJsArray should not be explicit ?
      .compose(Path[JsValue]())(Rule { case JsArray(is) => Success(is) })
        .compose(Path[JsValue]())(Constraints.seq(r))

  // Is that thing really just a Lens ?
  implicit def pickInJson[O](p: Path[JsValue])(implicit m: Rule[JsValue, O]): Rule[JsValue, O] =
    Rule { (json: JsValue) =>
      pathToJsPath(p)(json) match {
        case Nil => Failure(Seq(Path[JsValue]() -> Seq(ValidationError("validation.required"))))
        case js :: _ => Success(js)
      }
    }.compose(Path[JsValue]())(m)

}