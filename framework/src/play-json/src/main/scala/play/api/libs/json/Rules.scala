package play.api.libs.json

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
  import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode}
  private def pathToJsPath(p: Path) =
    play.api.libs.json.JsPath(p.path.map{
      case KeyPathNode(key) => JSKeyPathNode(key)
      case IdxPathNode(i) => JIdxPathNode(i)
    })

  private def jsonAs[T](f: PartialFunction[JsValue, Validation[ValidationError, T]])(args: Any*) =
    Rule.fromMapping[JsValue, T](
      f.orElse{ case j => Failure(Seq(ValidationError("validation.type-mismatch", args: _*)))
    })

  def string = jsonAs[String] {
    case JsString(v) => Success(v)
  }("String")

  def boolean = jsonAs[Boolean]{
    case JsBoolean(v) => Success(v)
  }("Boolean")

  // Note: Mappings of JsNumber to Number are validating that the JsNumber is indeed valid
  // in the target type. i.e: JsNumber(4.5) is not considered parseable as an Int.
  // That's a bit stricter than the "old" Read, which just cast to the target type, possibly loosing data.
  def int = jsonAs[Int]{
    case JsNumber(v) if v.isValidInt => Success(v.toInt)
  }("Int")

  def short = jsonAs[Short]{
    case JsNumber(v) if v.isValidShort => Success(v.toShort)
  }("Short")

  def long = jsonAs[Long]{
    case JsNumber(v) if v.isValidLong => Success(v.toLong)
  }("Long")

  def jsNumber = jsonAs[JsNumber]{
    case v@JsNumber(_) => Success(v)
  }("Number")

  def jsBoolean = jsonAs[JsBoolean]{
    case v@JsBoolean(_) => Success(v)
  }("Boolean")

  def jsString = jsonAs[JsString] {
    case v@JsString(_) => Success(v)
  }("String")

  def jsObject = jsonAs[JsObject] {
    case v@JsObject(_) => Success(v)
  }("Object")

  def jsArray = jsonAs[JsArray] {
    case v@JsArray(_) => Success(v)
  }("Array")

  // BigDecimal.isValidFloat is buggy, see [SI-6699]
  import java.{lang => jl}
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Float.toString(d), bd.mc)) == 0
  }
  def float = jsonAs[Float] {
    case JsNumber(v) if isValidFloat(v) => Success(v.toFloat)
  }("Float")

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Double.toString(d), bd.mc)) == 0
  }
  def double =jsonAs[Double] {
    case JsNumber(v) if isValidDouble(v) => Success(v.toDouble)
  }("Double")

  def bigDecimal = jsonAs[BigDecimal] {
    case JsNumber(v) => Success(v)
  }("BigDecimal")

  import java.{ math => jm }
  def javaBigDecimal = jsonAs[jm.BigDecimal] {
    case JsNumber(v) => Success(v.bigDecimal)
  }("BigDecimal")

  def jsNull = isJsNull[JsValue]

  private def isJsNull[J] = Rule.fromMapping[J, J]{
    case JsNull => Success(JsNull)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "null")))
  }

  override def option[J, O](r: Rule[J, O], noneValues: Rule[J, J]*)(implicit pick: Path => Rule[JsValue, J]): Path => Rule[JsValue, Option[O]]
    = super.option[J, O](r, (isJsNull[J] +: noneValues):_*)

  def map[O](r: Rule[JsValue, O]): Rule[JsValue, Map[String, O]] =
    super.map[JsValue, O](r, jsObject.fmap{ case JsObject(fs) => fs })

  // Is that thing really just a Lens ?
  implicit def pickInJson(p: Path) =
    Rule[JsValue, JsValue] { json =>
      pathToJsPath(p)(json) match {
        case Nil => Failure(Seq(Path() -> Seq(ValidationError("validation.required"))))
        case js :: _ => Success(js)
      }
    }

  implicit def pickSInJson(p: Path) =
    pickInJson(p).compose(jsArray).fmap{ case JsArray(fs) => fs }

}