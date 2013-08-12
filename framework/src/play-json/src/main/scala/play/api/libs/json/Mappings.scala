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

  implicit def jsonAsString: Mapping[ValidationError, JsValue, String] = {
    case JsString(v) => Success(v)
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "String")))
  }

  implicit def jsonAsInt: Mapping[ValidationError, JsValue, Int] = {
    case JsNumber(v) => Success(v.toInt) // XXX
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Int")))
  }

  implicit def jsonAsSeq[O](implicit m: Mapping[ValidationError, JsValue, O]): Mapping[ValidationError, JsValue, Seq[O]] = {
    case JsArray(vs) => Validation.sequence(vs.map(m))
    case _ => Failure(Seq(ValidationError("validation.type-mismatch", "Array")))
  }

  implicit def pickInJson[O](p: Path[JsValue])(implicit m: Mapping[ValidationError, JsValue, O]): Mapping[ValidationError, JsValue, O] = { json =>
    val v: Validation[ValidationError, JsValue] = pathToJsPath(p)(json) match {
      case Nil => Failure(Seq(ValidationError("validation.required")))
      case js :: _ => Success(js)
    }
    v.flatMap(m)
  }

}