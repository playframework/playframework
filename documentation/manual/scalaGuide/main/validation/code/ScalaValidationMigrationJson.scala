/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationMigrationJsonSpec extends Specification {

  "Scala Json migration" should {

    //#migration-creature
    case class Creature(
      name: String,
      isDead: Boolean,
      weight: Float)
    //#migration-creature

    "validate using the old api" in {
      //#migration-creature-old
      import play.api.libs.json._
      import play.api.libs.functional.syntax._

      implicit val creatureReads = (
        (__ \ "name").read[String] ~
        (__ \ "isDead").read[Boolean] ~
        (__ \ "weight").read[Float]
      )(Creature.apply _)

      val js = Json.obj( "name" -> "gremlins", "isDead" -> false, "weight" -> 1.0F)
      Json.fromJson[Creature](js) === JsSuccess(Creature("gremlins",false,1.0F))
      //#migration-creature-old
    }

    "validate using the new api" in {
      //#migration-creature-new
      import play.api.libs.json._
      import play.api.data.mapping._

      implicit val creatureRule = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (
          (__ \ "name").read[String] ~
          (__ \ "isDead").read[Boolean] ~
          (__ \ "weight").read[Float]
        )(Creature.apply _)
      }

      val js = Json.obj( "name" -> "gremlins", "isDead" -> false, "weight" -> 1.0F)
      From[JsValue, Creature](js) === Success(Creature("gremlins",false,1.0F))
      //#migration-creature-new
    }

    "read nullable" in {
      //#migration-readNullable
      import play.api.libs.json._
      import play.api.data.mapping._

      val nullableStringRule = From[JsValue]{ __ =>
        import play.api.data.mapping.json.Rules._
        (__ \ "foo").read[Option[String]]
      }

      val js1 = Json.obj("foo" -> "bar")
      val js2 = Json.obj("foo" -> JsNull)
      val js3 = Json.obj()

      nullableStringRule.validate(js1) === Success(Some("bar"))
      nullableStringRule.validate(js2) === Success(None)
      nullableStringRule.validate(js3) === Success(None)
      //#migration-readNullable
    }

    "keepAnd" in {
      "old" in {
        import play.api.libs.json._
        import Reads._
        import play.api.libs.functional.syntax._
        //#migration-keepAnd-old
        (JsPath \ "key1").read[String](email keepAnd minLength[String](5))
        //#migration-keepAnd-old
        success
      }

      "new" in {
        //#migration-keepAnd-new
        import play.api.libs.json._
        import play.api.data.mapping._

        From[JsValue]{ __ =>
          import play.api.data.mapping.json.Rules._
          (__ \ "key1").read(email |+| minLength(5))
        }
        //#migration-keepAnd-new
        success
      }
    }

    "lazy reads" in {
      "old" in {
        //#migration-lazy-old
        import play.api.libs.json._
        import play.api.libs.functional.syntax._

        case class User(id: Long, name: String, friend: Option[User] = None)

        implicit lazy val UserReads: Reads[User] = (
          (__ \ 'id).read[Long] and
          (__ \ 'name).read[String] and
          (__ \ 'friend).lazyReadNullable(UserReads)
        )(User.apply _)

        val js = Json.obj(
          "id" -> 123L,
          "name" -> "bob",
          "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> JsNull))

        Json.fromJson[User](js) === JsSuccess(User(123L, "bob", Some(User(124L, "john", None))))
        //#migration-lazy-old
      }

      "new" in {
        //#migration-lazy-new
        import play.api.libs.json._
        import play.api.data.mapping._

        case class User(id: Long, name: String, friend: Option[User] = None)

        implicit lazy val userRule: Rule[JsValue, User] = From[JsValue]{ __ =>
          import play.api.data.mapping.json.Rules._
          (
            (__ \ "id").read[Long] and
            (__ \ "name").read[String] and
            (__ \ "friend").read(option(userRule))
          )(User.apply _)
        }

        val js = Json.obj(
          "id" -> 123L,
          "name" -> "bob",
          "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> JsNull))

        From[JsValue, User](js) === Success(User(123L, "bob", Some(User(124L, "john", None))))
        //#migration-lazy-new
      }
    }

  }
}
