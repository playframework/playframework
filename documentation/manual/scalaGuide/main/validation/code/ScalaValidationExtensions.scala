/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationExtensionsSpec extends Specification {


  import play.api.libs.json._
  object RulesExtract {

    import play.api.data.mapping._

    import play.api.data.mapping._ // We need that import to shadow Json PathNodes types
    import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode}

    import RealRules.pathToJsPath

    //#extensions-rules
    implicit def pickInJson[O](p: Path): Rule[JsValue, JsValue] =
      Rule[JsValue, JsValue] { json =>
        pathToJsPath(p)(json) match {
          case Nil => Failure(Seq(Path -> Seq(ValidationError("error.required"))))
          case js :: _ => Success(js)
        }
      }
    //#extensions-rules
  }

  object Writes {
    import play.api.data.mapping._

    //#extensions-writes
    implicit def writeJson[I](path: Path)(implicit w: Write[I, JsValue]): Write[I, JsObject] = ???
    //#extensions-writes

    //#extensions-writes-anyval
    implicit def anyval[T <: AnyVal] = ???
    //#extensions-writes-anyval


    //#extensions-writes-monoid
    import play.api.libs.functional.Monoid
    implicit def jsonMonoid = new Monoid[JsObject] {
      def append(a1: JsObject, a2: JsObject) = a1 deepMerge a2
      def identity = Json.obj()
    }
    //#extensions-writes-monoid

  }

  object RealRules {
    import play.api.data.mapping._

    import play.api.data.mapping._ // We need that import to shadow Json PathNodes types
    import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode}

     def pathToJsPath(p: Path) =
      play.api.libs.json.JsPath(p.path.map{
        case KeyPathNode(key) => JSKeyPathNode(key)
        case IdxPathNode(i) => JIdxPathNode(i)
      })

      //#extensions-rules-primitives
      private def jsonAs[T](f: PartialFunction[JsValue, Validation[ValidationError, T]])(args: Any*) =
        Rule.fromMapping[JsValue, T](
          f.orElse{ case j => Failure(Seq(ValidationError("validation.invalid", args: _*)))
        })

      implicit def string = jsonAs[String] {
        case JsString(v) => Success(v)
      }("String")

      implicit def boolean = jsonAs[Boolean]{
        case JsBoolean(v) => Success(v)
      }("Boolean")
      // ...
      //#extensions-rules-primitives


     //#extensions-rules-gen
    implicit def pickInJson[O](p: Path)(implicit r: Rule[JsValue, O]): Rule[JsValue, O] =
    Rule[JsValue, JsValue] { json =>
      pathToJsPath(p)(json) match {
        case Nil => Failure(Seq(Path -> Seq(ValidationError("error.required"))))
        case js :: _ => Success(js)
      }
    }.compose(r)
    //#extensions-rules-gen
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

    "validation optionnal" in {
      import play.api.libs.json._
      import play.api.data.mapping._

      //#extensions-rules-opt
      val maybeEmail = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (__ \ "email").read(option(email))
      }

      maybeEmail.validate(Json.obj("email" -> "foo@bar.com")) === Success(Some("foo@bar.com"))
      maybeEmail.validate(Json.obj("email" -> "baam!")) === Failure(Seq((Path \ "email", Seq(ValidationError("error.email")))))
      maybeEmail.validate(Json.obj("email" -> JsNull)) === Success(None)
      maybeEmail.validate(Json.obj()) === Success(None)
      //#extensions-rules-opt

      //#extensions-rules-opt-int
      val maybeAge = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (__ \ "age").read[Option[Int]]
      }

      // maybeEmail.validate(Json.obj("age" -> 28)) === Success(Some(28))
      maybeAge.validate(Json.obj("age" -> "baam!")) === Failure(Seq((Path \ "age", Seq(ValidationError("error.invalid", "Int")))))
      maybeAge.validate(Json.obj("age" -> JsNull)) === Success(None)
      maybeAge.validate(Json.obj()) === Success(None)
      //#extensions-rules-opt-int
    }

    "validation recursive" in {
      import play.api.data.mapping._

      case class RecUser(name: String, friends: Seq[RecUser] = Nil)

      //#extensions-rules-recursive
      val u = RecUser(
        "bob",
        Seq(RecUser("tom")))

      lazy val w: Rule[JsValue, RecUser] = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        ((__ \ "name").read[String] ~
         (__ \ "friends").read(seq(w)))(RecUser.apply _) // !!! recursive rule definition
      }
      //#extensions-rules-recursive

      val m = Json.obj(
        "name" -> "bob",
        "friends" -> Seq(Json.obj("name" -> "tom", "friends" -> Seq[JsObject]())))

      w.validate(m) === Success(u)
    }

  }
}
