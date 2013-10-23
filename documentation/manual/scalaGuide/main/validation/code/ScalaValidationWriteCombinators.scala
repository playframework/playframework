/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationWriteCombinatorsSpec extends Specification {

  //#combinators-location
  import play.api.data.mapping._
  val location: Path = Path \ "user" \ "friend"
  //#combinators-location

  "Scala Write combinators" should {

    "Writes to JsObject" in {
      //#write-serializeFriend-import
      import play.api.data.mapping.json.Writes._
      //#write-serializeFriend-import

      //#write-serializeFriend
      import play.api.libs.json._
      import play.api.data.mapping._

      val location: Path = Path \ "user" \ "friend"
      val serializeFriend: Write[JsValue, JsObject] = location.write[JsValue, JsObject]
      //#write-serializeFriend

      //#write-serializeFriend-test
      serializeFriend.writes(JsString("Julien")) === Json.parse("""{"user":{"friend":"Julien"}}""")
      //#write-serializeFriend-test
    }

    "coerce type" in {
      //#write-coerce
      import play.api.libs.json._
      import play.api.data.mapping._
      import play.api.data.mapping.json.Writes._

      val agejs = (Path \ "user" \ "age").write[JsValue, JsObject]
      //#write-coerce

      //#write-coerce-test
      agejs.writes(JsNumber(28)) === Json.parse("""{"user":{"age":28}}""")
      //#write-coerce-test

      //#write-coerce-age
      val age = (Path \ "user" \ "age").write[Int, JsObject]
      //#write-coerce-age

      //#write-coerce-age-test
      age.writes(28) === Json.parse("""{"user":{"age":28}}""")
      //#write-coerce-age-test
    }

    "full example" in {
      //#write-full
      import play.api.libs.json._
      import play.api.data.mapping._
      import play.api.data.mapping.json.Writes._

      val age = (Path \ "user" \ "age").write[Int, JsObject]
      age.writes(28) === Json.parse("""{"user":{"age":28}}""")
      //#write-full
    }

    "combine write" in {
      //#write-combine-user
      case class User(
        name: String,
        age: Int,
        email: Option[String],
        isAlive: Boolean)
      //#write-combine-user

      //#write-combine
      import play.api.libs.json._
      import play.api.data.mapping._
      import play.api.data.mapping.json.Writes._
      import play.api.libs.functional.syntax.unlift

      val userWrite = To[JsObject] { __ =>
        import play.api.data.mapping.json.Writes._
        ((__ \ "name").write[String] and
         (__ \ "age").write[Int] and
         (__ \ "email").write[Option[String]] and
         (__ \ "isAlive").write[Boolean])(unlift(User.unapply _))
      }
      //#write-combine

      //#write-combine-test
      userWrite.writes(User("Julien", 28, None, true)) === Json.parse("""{"name":"Julien","age":28,"isAlive":true}""")
      //#write-combine-test
    }

  }
}
