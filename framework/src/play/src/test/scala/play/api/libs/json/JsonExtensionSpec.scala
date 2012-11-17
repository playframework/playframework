package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._

case class User(age: Int, name: String)
case class Dog(name: String, master: User)

case class Cat(name: String)

case class RecUser(name: String, cat: Option[Cat] = None, hobbies: List[String] = List(), friends: List[RecUser] = List())

case class User1(name: String, friend: Option[User1] = None)

object JsonExtensionSpec extends Specification {

  "JsonExtension" should {
    "create a reads[User]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._

      //object User {def apply(age:Int):User = User(age,"")}
      implicit val userReads = Json.reads[User]

      Json.fromJson[User](Json.obj("name" -> "toto", "age" -> 45)) must beEqualTo(JsSuccess(User(45, "toto")))
    }

    "create a writes[User]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._

      implicit val userWrites = Json.writes[User]

      Json.toJson(User(45, "toto")) must beEqualTo(Json.obj("name" -> "toto", "age" -> 45))
    }

    "create a format[User]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._

      implicit val userFormat = Json.format[User]

      Json.fromJson[User](Json.obj("name" -> "toto", "age" -> 45)) must beEqualTo(JsSuccess(User(45, "toto")))
      Json.toJson(User(45, "toto")) must beEqualTo(Json.obj("name" -> "toto", "age" -> 45))
    }


    "create a reads[Dog]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._

      implicit val userReads = Json.reads[User]
      implicit val dogReads = Json.reads[Dog]

      Json.fromJson[Dog](
        Json.obj(
          "name" -> "medor", 
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      ) must beEqualTo(JsSuccess(Dog("medor", User(45, "toto"))))
      
    }

    "create a writes[Dog]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Writes._

      implicit val userWrites = Json.writes[User]
      implicit val dogWrites = Json.writes[Dog]

      Json.toJson(Dog("medor", User(45, "toto"))) must beEqualTo(
        Json.obj(
          "name" -> "medor", 
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      )
    }

    "create a format[Dog]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._

      implicit val userFormat = Json.format[User]
      implicit val dogFormat = Json.format[Dog]

      Json.fromJson[Dog](
        Json.obj(
          "name" -> "medor", 
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      ) must beEqualTo(JsSuccess(Dog("medor", User(45, "toto"))))

      Json.toJson(Dog("medor", User(45, "toto"))) must beEqualTo(
        Json.obj(
          "name" -> "medor", 
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      )
    }

    "create a reads[RecUser]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._

      implicit val catReads = Json.reads[Cat]
      implicit val recUserReads = Json.reads[RecUser]

      Json.fromJson[RecUser](
        Json.obj(
          "name" -> "bob", 
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
        )
      ) must beEqualTo(
        JsSuccess(
          RecUser(
            "bob", 
            Some(Cat("minou")),
            List("bobsleig", "manhunting"),
            List(RecUser("tom"))
          )
        )
      )

    }

    "create a writes[RecUser]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Writes._

      implicit val catWrites = Json.writes[Cat]
      implicit val recUserWrites = Json.writes[RecUser]

      Json.toJson(
        RecUser(
          "bob", 
          Some(Cat("minou")),
          List("bobsleig", "manhunting"),
          List(RecUser("tom"))
        )
      ) must beEqualTo(
        Json.obj(
          "name" -> "bob", 
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
        )
      )

    }

    "create a format[RecUser]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._

      implicit val catFormat = Json.format[Cat]
      implicit val recUserFormat = Json.format[RecUser]

      Json.fromJson[RecUser](
        Json.obj(
          "name" -> "bob", 
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
        )
      ) must beEqualTo(
        JsSuccess(
          RecUser(
            "bob", 
            Some(Cat("minou")),
            List("bobsleig", "manhunting"),
            List(RecUser("tom"))
          )
        )
      )

      Json.toJson(
        RecUser(
          "bob", 
          Some(Cat("minou")),
          List("bobsleig", "manhunting"),
          List(RecUser("tom"))
        )
      ) must beEqualTo(
        Json.obj(
          "name" -> "bob", 
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
        )
      )

    }

    "create a reads[User1]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional._
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._

      implicit val userReads = Json.reads[User1]

      Json.fromJson[User1](
        Json.obj(
          "name" -> "bob", 
          "friend" -> Json.obj( "name" -> "tom" )
        )
      ) must beEqualTo(
        JsSuccess(
          User1(
            "bob", 
            Some(User1("tom"))
          )
        )
      )
    }

    "create a writes[User1]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._

      implicit val userWrites = Json.writes[User1]


      Json.toJson(
        User1(
          "bob", 
          Some(User1("tom"))
        )
      ) must beEqualTo(
        Json.obj(
          "name" -> "bob", 
          "friend" -> Json.obj( "name" -> "tom" )
        )
      )

    }

    "create a format[User1]" in {
      import play.api.libs.json.Json
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._
      import play.api.libs.json.Writes._
      import play.api.libs.json.Format._

      implicit val userFormat = Json.format[User1]

      Json.fromJson[User1](
        Json.obj(
          "name" -> "bob", 
          "friend" -> Json.obj( "name" -> "tom" )
        )
      ) must beEqualTo(
        JsSuccess(
          User1(
            "bob", 
            Some(User1("tom"))
          )
        )
      )

      Json.toJson(
        User1(
          "bob", 
          Some(User1("tom"))
        )
      ) must beEqualTo(
        Json.obj(
          "name" -> "bob", 
          "friend" -> Json.obj( "name" -> "tom" )
        )
      )

    }
  }

}
