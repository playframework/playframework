/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationExtensionsSpec extends Specification {


  import play.api.libs.json._
  object Rules {

    import play.api.data.mapping._

    import play.api.data.mapping._ // We need that import to shadow Json PathNodes types
    import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode}

    private def pathToJsPath(p: Path) =
    play.api.libs.json.JsPath(p.path.map{
      case KeyPathNode(key) => JSKeyPathNode(key)
      case IdxPathNode(i) => JIdxPathNode(i)
    })

    //#extensions-rules
    implicit def pickInJson[O](p: Path): Rule[JsValue, JsValue] =
      Rule[JsValue, JsValue] { json =>
        pathToJsPath(p)(json) match {
          case Nil => Failure(Seq(Path -> Seq(ValidationError("validation.required"))))
          case js :: _ => Success(js)
        }
      }
    //#extensions-rules
  }

  "Scala extensions" should {

    "validate jsvalue" in {
      //#extensions-rules-jsvalue
      import play.api.libs.json._
      import play.api.data.mapping._

      val js = Json.obj(
        "field1" -> "alpha",
        "field2" -> 123L,
        "field3" -> Json.obj(
          "field31" -> "beta",
          "field32"-> 345))

      val pick = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (__ \ "field2").read[JsValue]
      }

      pick.validate(js) === Success(JsNumber(123))
      //#extensions-rules-jsvalue
    }

  }
}
