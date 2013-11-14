/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

//#validate-recursive-data
case class User(
  name: String,
  age: Int,
  email: Option[String],
  isAlive: Boolean,
  friend: Option[User])
//#validate-recursive-data

object ScalaValidationCookbookSpec extends Specification {

  "Scala Validation Cookbook" should {

    "Validate case classes" in {
      //#validate-case-class
      import play.api.libs.json._
      import play.api.data.mapping._

      case class Creature(
        name: String,
        isDead: Boolean,
        weight: Float)

      implicit val creatureRule = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (
          (__ \ "name").read[String] ~
          (__ \ "isDead").read[Boolean] ~
          (__ \ "weight").read[Float]
        )(Creature.apply _)
      }

      val js = Json.obj( "name" -> "gremlins", "isDead" -> false, "weight" -> 1.0f)
      From[JsValue, Creature](js) == Success(Creature("gremlins", false, 1.0f))

      From[JsValue, Creature](Json.obj()) ===
       Failure(Seq(
        (Path \ "name", Seq(ValidationError("error.required"))),
        (Path \ "isDead", Seq(ValidationError("error.required"))),
        (Path \ "weight", Seq(ValidationError("error.required")))))
      //#validate-case-class
    }

    "Validate dependent fields" in {
      //#validate-dependent
      import play.api.libs.json._
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._

      import play.api.data.mapping._

      val passRule = From[JsValue] { __ =>
        import play.api.data.mapping.json.Rules._
        //#validate-dependent1
        ((__ \ "password").read(notEmpty) ~
         (__ \ "verify").read(notEmpty)).tupled
        //#validate-dependent1
        //#validate-dependent2
          .compose(Rule.uncurry(Rules.equalTo[String])
        //#validate-dependent2
        //#validate-dependent3
          .repath(_ => (Path \ "verify")))
        //#validate-dependent3
      }
      //#validate-dependent

      //#validate-dependent-tests
      passRule.validate(Json.obj("password" -> "foo", "verify" -> "foo")) === Success("foo")
      passRule.validate(Json.obj("password" -> "", "verify" -> "foo")) === Failure(Seq((Path \ "password", Seq(ValidationError("error.required")))))
      passRule.validate(Json.obj("password" -> "foo", "verify" -> "")) === Failure(Seq((Path \ "verify", Seq(ValidationError("error.required")))))
      passRule.validate(Json.obj("password" -> "", "verify" -> "")) === Failure(Seq((Path \ "password", Seq(ValidationError("error.required"))), (Path \ "verify", List(ValidationError("error.required")))))
      passRule.validate(Json.obj("password" -> "foo", "verify" -> "bar")) === Failure(Seq((Path \ "verify", Seq(ValidationError("error.equals", "foo")))))
      //#validate-dependent-tests
    }

    "Validate recursive type" in {

      "using explicit declaration" in {
        //#validate-recursive
        import play.api.libs.json._
        import play.api.data.mapping._

        // Note the lazy keyword, and the explicit typing
        implicit lazy val userRule: Rule[JsValue, User] = From[JsValue] { __ =>
          import play.api.data.mapping.json.Rules._

          ((__ \ "name").read[String] and
           (__ \ "age").read[Int] and
           (__ \ "email").read[Option[String]] and
           (__ \ "isAlive").read[Boolean] and
           (__ \ "friend").read[Option[User]])(User.apply _)
        }
        //#validate-recursive
        success
      }

      "using explicit declaration" in {
        //#validate-recursive-macro
        import play.api.libs.json._
        import play.api.data.mapping._
        import play.api.data.mapping.json.Rules._

        // Note the lazy keyword, and the explicit typing
        implicit lazy val userRule: Rule[JsValue, User] = Rule.gen[JsValue, User]
        //#validate-recursive-macro
        success
      }
    }

    "read keys" in {
      //#validate-read-keys
      import play.api.libs.json._
      import play.api.data.mapping._

      val js = Json.parse("""
      {
        "values": [
          { "foo": "bar" },
          { "bar": "baz" }
        ]
      }
      """)

      val r = From[JsValue] { __ =>
        import play.api.data.mapping.json.Rules._

        val tupleR = Rule.fromMapping[JsValue, (String, String)]{
          case JsObject(Seq((key, JsString(value)))) =>  Success(key -> value)
          case _ => Failure(Seq(ValidationError("BAAAM")))
        }

        (__ \ "values").read(seq(tupleR))
      }

      r.validate(js) === Success(Seq(("foo", "bar"), ("bar", "baz")))
      //#validate-read-keys
    }

    "Validate recursive" in {
      import play.api.libs.json._
      import play.api.data.mapping._

      //#validate-recursive-def
       trait A
      case class B(foo: Int) extends A
      case class C(bar: Int) extends A

      val b = Json.obj("name" -> "B", "foo" -> 4)
      val c = Json.obj("name" -> "C", "bar" -> 6)
      val e = Json.obj("name" -> "E", "eee" -> 6)
      //#validate-recursive-def

      "testing all implementations" in {
        //#validate-recursive-testall
        val rb: Rule[JsValue, A] = From[JsValue]{ __ =>
          import play.api.data.mapping.json.Rules._
          (__ \ "name").read(Rules.equalTo("B")) ~> (__ \ "foo").read[Int].fmap(B.apply _)
        }

        val rc: Rule[JsValue, A] = From[JsValue]{ __ =>
          import play.api.data.mapping.json.Rules._
          (__ \ "name").read(Rules.equalTo("C")) ~> (__ \ "bar").read[Int].fmap(C.apply _)
        }

        val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
        val rule = rb orElse rc orElse Rule(_ => typeFailure)

        rule.validate(b) === Success(B(4))
        rule.validate(c) === Success(C(6))
        rule.validate(e) === Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
        //#validate-recursive-testall
      }

      "testing field value" in {
         //#validate-recursive-field
         val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))

         val rule = From[JsValue] { __ =>
          import play.api.data.mapping.json.Rules._
          (__ \ "name").read[String].flatMap[A] {
            case "B" => (__ \ "foo").read[Int].fmap(B.apply _)
            case "C" => (__ \ "bar").read[Int].fmap(C.apply _)
            case _ => Rule(_ => typeFailure)
          }
        }

        rule.validate(b) === Success(B(4))
        rule.validate(c) === Success(C(6))
        rule.validate(e) === Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
        //#validate-recursive-field
      }
    }

    "Write case class" in {
      //#write-case
      import play.api.libs.json._
      import play.api.data.mapping._
      import play.api.libs.functional.syntax.unlift

      case class Creature(
        name: String,
        isDead: Boolean,
        weight: Float)

      implicit val creatureWrite = To[JsObject]{ __ =>
        import play.api.data.mapping.json.Writes._
        (
          (__ \ "name").write[String] ~
          (__ \ "isDead").write[Boolean] ~
          (__ \ "weight").write[Float]
        )(unlift(Creature.unapply _))
      }

      To[Creature, JsObject](Creature("gremlins", false, 1f)) === Json.parse("""{"name":"gremlins","isDead":false,"weight":1.0}""")
      //#write-case
    }

    "Add values to a write" in {
      //#write-static
      import play.api.libs.json._
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._

      import play.api.data.mapping._

      case class LatLong(lat: Float, long: Float)

      implicit val latLongWrite = {
        import play.api.data.mapping.json.Writes._
        To[JsObject] { __ =>
          ((__ \ "lat").write[Float] ~
           (__ \ "long").write[Float])(unlift(LatLong.unapply _))
        }
      }

      case class Point(coords: LatLong)

      implicit val pointWrite = {
        import play.api.data.mapping.json.Writes._
        To[JsObject] { __ =>
          ((__ \ "coords").write[LatLong] ~
           (__ \ "type").write[String])((_: Point).coords -> "point")
        }
      }

      val p = Point(LatLong(123.3F, 334.5F))
      pointWrite.writes(p) === Json.parse("""{"coords":{"lat":123.3,"long":334.5},"type":"point"}""")
      //#write-static
    }
  }

}
