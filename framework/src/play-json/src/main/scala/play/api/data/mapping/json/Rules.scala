package play.api.data.mapping.json

import play.api.libs.json._

// Backward compat
trait WithRepath[A] {
  import play.api.data.mapping._
  val self: Validation[(JsPath, Seq[ValidationError]), A]

  def repath(path: JsPath): JsResult[A] = self match {
    case JsSuccess(a, p) => JsSuccess(a, path ++ p)
    case JsError(es) => JsError(es.map { case (p, s) => path ++ p -> s })
  }
}

object Rules extends play.api.data.mapping.DefaultRules[JsValue] {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._

  import play.api.data.mapping._ // We need that import to shadow Json PathNodes types
  import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode }
  private def pathToJsPath(p: Path) =
    play.api.libs.json.JsPath(p.path.map {
      case KeyPathNode(key) => JSKeyPathNode(key)
      case IdxPathNode(i) => JIdxPathNode(i)
    })

  private def jsonAs[T](f: PartialFunction[JsValue, Validation[ValidationError, T]])(msg: String, args: Any*) =
    Rule.fromMapping[JsValue, T](
      f.orElse {
        case j => Failure(Seq(ValidationError(msg, args: _*)))
      })

  implicit def stringR = jsonAs[String] {
    case JsString(v) => Success(v)
  }("error.invalid", "String")

  implicit def booleanR = jsonAs[Boolean] {
    case JsBoolean(v) => Success(v)
  }("error.invalid", "Boolean")

  // Note: Mappings of JsNumber to Number are validating that the JsNumber is indeed valid
  // in the target type. i.e: JsNumber(4.5) is not considered parseable as an Int.
  // That's a bit stricter than the "old" Read, which just cast to the target type, possibly loosing data.
  implicit def intR = jsonAs[Int] {
    case JsNumber(v) if v.isValidInt => Success(v.toInt)
  }("error.number", "Int")

  implicit def shortR = jsonAs[Short] {
    case JsNumber(v) if v.isValidShort => Success(v.toShort)
  }("error.number", "Short")

  implicit def longR = jsonAs[Long] {
    case JsNumber(v) if v.isValidLong => Success(v.toLong)
  }("error.number", "Long")

  implicit def jsNumberR = jsonAs[JsNumber] {
    case v @ JsNumber(_) => Success(v)
  }("error.number", "Number")

  implicit def jsBooleanR = jsonAs[JsBoolean] {
    case v @ JsBoolean(_) => Success(v)
  }("error.invalid", "Boolean")

  implicit def jsStringR = jsonAs[JsString] {
    case v @ JsString(_) => Success(v)
  }("error.invalid", "String")

  implicit def jsObjectR = jsonAs[JsObject] {
    case v @ JsObject(_) => Success(v)
  }("error.invalid", "Object")

  implicit def jsArrayR = jsonAs[JsArray] {
    case v @ JsArray(_) => Success(v)
  }("error.invalid", "Array")

  // BigDecimal.isValidFloat is buggy, see [SI-6699]
  import java.{ lang => jl }
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Float.toString(d), bd.mc)) == 0
  }
  implicit def floatR = jsonAs[Float] {
    case JsNumber(v) if isValidFloat(v) => Success(v.toFloat)
  }("error.number", "Float")

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Double.toString(d), bd.mc)) == 0
  }
  implicit def doubleR = jsonAs[Double] {
    case JsNumber(v) if isValidDouble(v) => Success(v.toDouble)
  }("error.number", "Double")

  implicit def bigDecimal = jsonAs[BigDecimal] {
    case JsNumber(v) => Success(v)
  }("error.number", "BigDecimal")

  import java.{ math => jm }
  implicit def javaBigDecimal = jsonAs[jm.BigDecimal] {
    case JsNumber(v) => Success(v.bigDecimal)
  }("error.number", "BigDecimal")

  implicit val jsNullR = jsonAs[JsNull.type] {
    case JsNull => Success(JsNull)
  }("error.invalid", "null")

  implicit def ooo[O](p: Path)(implicit pick: Path => RuleLike[JsValue, JsValue], coerce: RuleLike[JsValue, O]): Rule[JsValue, Option[O]] =
    optionR(Rule.zero[O])(pick, coerce)(p)

  def optionR[J, O](r: => RuleLike[J, O], noneValues: RuleLike[JsValue, JsValue]*)(implicit pick: Path => RuleLike[JsValue, JsValue], coerce: RuleLike[JsValue, J]): Path => Rule[JsValue, Option[O]] =
    super.opt[J, O](r, (jsNullR.fmap(n => n: JsValue) +: noneValues): _*)

  implicit def mapR[O](implicit r: RuleLike[JsValue, O]): Rule[JsValue, Map[String, O]] =
    super.mapR[JsValue, O](r, jsObjectR.fmap { case JsObject(fs) => fs })

  implicit def JsValue[O](implicit r: RuleLike[JsObject, O]): Rule[JsValue, O] =
    jsObjectR.compose(r)

  implicit def pickInJson[II <: JsValue, O](p: Path)(implicit r: RuleLike[JsValue, O]): Rule[II, O] =
    Rule[II, JsValue] { json =>
      pathToJsPath(p)(json) match {
        case Nil => Failure(Seq(Path -> Seq(ValidationError("error.required"))))
        case js :: _ => Success(js)
      }
    }.compose(r)

  // // XXX: a bit of boilerplate
  private def pickInS[T](implicit r: RuleLike[Seq[JsValue], T]): Rule[JsValue, T] =
    jsArrayR.fmap { case JsArray(fs) => fs }.compose(r)
  implicit def pickSeq[O](implicit r: RuleLike[JsValue, O]) = pickInS(seqR[JsValue, O])
  implicit def pickSet[O](implicit r: RuleLike[JsValue, O]) = pickInS(setR[JsValue, O])
  implicit def pickList[O](implicit r: RuleLike[JsValue, O]) = pickInS(listR[JsValue, O])
  implicit def pickArray[O: scala.reflect.ClassTag](implicit r: RuleLike[JsValue, O]) = pickInS(arrayR[JsValue, O])
  implicit def pickTraversable[O](implicit r: RuleLike[JsValue, O]) = pickInS(traversableR[JsValue, O])

}