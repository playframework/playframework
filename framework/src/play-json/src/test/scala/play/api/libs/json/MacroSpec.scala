package play.api.libs.json

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

import play.api.data.mapping.{ Rules => _, _}
import play.api.data.mapping.{ json => js }
import js._

object MacroSpec extends Specification {

  case class User(age: Int, name: String)
  case class Dog(name: String, master: User)

  case class Cat(name: String)

  case class RecUser(name: String, cat: Option[Cat] = None, hobbies: Seq[String] = Seq(), friends: Seq[RecUser] = Seq())

  case class User1(name: String, friend: Option[User1] = None)

  case class UserMap(name: String, friends: Map[String, UserMap] = Map())

  case class Toto(name: String)
  case class Toto2(name: Option[String])
  case class Toto3(name: List[Double])
  case class Toto4(name: Set[Long])
  case class Toto5(name: Map[String, Int])
  case class Toto6(name: Seq[Dog])
  case class UserFail(name: String, bd: Toto)

  case class Id[A](id: A)
  case class C1[A](id: Id[A], name: String)

  case class X(
    _1: String, _2: String, _3: String, _4: String, _5: String,
    _6: String, _7: String, _8: String, _9: String, _10: String,
    _11: String, _12: String, _13: String, _14: String, _15: String,
    _16: String, _17: String, _18: String, _19: String, _20: String,
    _21: String
  )

  case class Program(id: Long, name: String, logoPath: Option[String], logoThumb: Option[String])
  object Program {
    def programs = List.empty[Program]
  }

  case class Person(name: String, age: Int)
  object Person {
    implicit val personRule = {
      import Rules._
      Rule.gen[JsValue, Person]
    }
    implicit val personWrite = {
      import js.Writes._
      Write.gen[Person, JsObject]
    }
  }

  case class Person2(names: List[String])

  object Person2{
    implicit val personRule = {
      import js.Rules._
      Rule.gen[JsValue, Person2]
    }
    implicit val personWrite = {
      import js.Writes._
      Write.gen[Person2, JsObject]
    }
  }

  "MappingMacros" should {

    "create a Rule[User]" in {
      import Rules._
      implicit val userReads = Rule.gen[JsValue, User]
      userReads.validate(Json.obj("name" -> "toto", "age" -> 45)) must beEqualTo(Success(User(45, "toto")))
    }

    "create a Write[User]" in {
      import js.Writes._
      implicit val userWrites = Write.gen[User, JsObject]
      userWrites.writes(User(45, "toto")) must beEqualTo(Json.obj("name" -> "toto", "age" -> 45))
    }

    "create a Rule[Dog]" in {
      import js.Rules._
      implicit val userRule = Rule.gen[JsValue, User]
      implicit val dogRule = Rule.gen[JsValue, Dog]

      dogRule.validate(
        Json.obj(
          "name" -> "medor",
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      ) must beEqualTo(Success(Dog("medor", User(45, "toto"))))

    }

    "create a Write[Dog]" in {
      import js.Writes._
      implicit val userWrite = Write.gen[User, JsObject]
      implicit val dogWrite = Write.gen[Dog, JsObject]

      dogWrite.writes(Dog("medor", User(45, "toto"))) must beEqualTo(
        Json.obj(
          "name" -> "medor",
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        )
      )
    }

    "create a Rule[RecUser]" in {
      import Rules._

      implicit val catRule = Rule.gen[JsValue, Cat]

      catRule.validate(
        Json.obj("name" -> "minou")
      ) must beEqualTo(Success(Cat("minou")))

      implicit lazy val recUserRule: Rule[JsValue, RecUser] =
        Rule.gen[JsValue, RecUser]

      recUserRule.validate(
        Json.obj(
          "name" -> "bob",
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
        )
      ) must beEqualTo(
        Success(
          RecUser(
            "bob",
            Some(Cat("minou")),
            List("bobsleig", "manhunting"),
            List(RecUser("tom"))
          )
        )
      )

    }

    "create a Write[RecUser]" in {
      import js.Writes._

      implicit val catWrite = Write.gen[Cat, JsObject]
      catWrite.writes(Cat("minou")) must beEqualTo(Json.obj("name" -> "minou"))

      implicit lazy val recUserWrite: Write[RecUser, JsValue] = Write.gen[RecUser, JsObject]

      recUserWrite.writes(
        RecUser(
          "bob",
          Some(Cat("minou")),
          Seq("bobsleig", "manhunting"),
          Seq(RecUser("tom"))
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

    // "create a Rule[User1]" in {
    //   import Rules._

    //   implicit lazy val userRule: Rule[JsValue, User1] = Rule.gen[JsValue, User1]

    //   userRule.validate(
    //     Json.obj(
    //       "name" -> "bob",
    //       "friend" -> Json.obj( "name" -> "tom" )
    //     )
    //   ) must beEqualTo(
    //     Success(
    //       User1(
    //         "bob",
    //         Some(User1("tom"))
    //       )
    //     )
    //   )
    // }


    "create a writes[User1]" in {
      import js.Writes._
      implicit lazy val userWrites: Write[User1, JsValue] = Write.gen[User1, JsObject]

      userWrites.writes(
        User1(
          "bob",
          Some(User1("tom")))
      ) must beEqualTo(
        Json.obj(
          "name" -> "bob",
          "friend" -> Json.obj("name" -> "tom" )))
    }

    "manage Boxed class" in {
      import play.api.libs.functional.syntax._
      import Rules._

      implicit def idRule[A]: Rule[A, Id[A]] =
        Rule.zero[A].fmap{ Id[A](_) }

      implicit def c1Rule[A](implicit rds: Rule[A, Id[A]], e: Path => Rule[JsValue, A]) =
        From[JsValue]{ __ =>
          ((__ \ "id").read(rds) and
           (__ \ "name").read[String])( (id, name) => C1[A](id, name) )
        }

      val js = Json.obj(
        "id" -> 123L,
        "name" -> "toto")

      c1Rule[Long].validate(js) must beEqualTo(Success(C1[Long](Id[Long](123L), "toto")))
    }

    // test to validate it doesn't compile if missing implicit
    /*
    "fail if missing " in {
      import Rules._
      implicit val userReads = Rule.gen[JsValue, UserFail]
      success
    }
    */

    "test 21 fields" in {
      "Rule" in {
        import Rules._
        implicit val XRule = Rule.gen[JsValue, X]
        success
      }

      "Write" in {
        import js.Writes._
        implicit val XWrites = Write.gen[X, JsObject]
        success
      }
    }

    "test inception with overriden object" in {
      import Rules._
      implicit val programFormat = Rule.gen[JsValue, Program]
      success
    }

    "test case class 1 field" in {
      "Rule" in {
        import Rules._
        implicit val totoRule = Rule.gen[JsValue, Toto]
        success
      }

      "Write" in {
        import js.Writes._
        implicit val totoWrite = Write.gen[Toto, JsObject]
        success
      }
    }

    "test case class 1 field option" in {
      "Rule" in {
        import Rules._
        implicit val toto2Rule = Rule.gen[JsValue, Toto2]
        success
      }

      "Write" in {
        import js.Writes._
        implicit val toto2Write = Write.gen[Toto2, JsObject]
        success
      }
    }

    "test case class 1 field list" in {
      "Rule" in {
        import Rules._
        implicit val toto3Rule = Rule.gen[JsValue, Toto3]
        success
      }

      "Write" in {
        import js.Writes._
        implicit val toto3Write = Write.gen[Toto3, JsObject]
        success
      }
    }

    "test case class 1 field set" in {
      "Rule" in {
        import Rules._
        implicit val toto4Rule = Rule.gen[JsValue, Toto4]
        success
      }

      "Write" in {
        import js.Writes._
        implicit val toto4Write = Write.gen[Toto4, JsObject]
        success
      }
    }

    "test case class 1 field map" in {
      "Rule" in {
        import Rules.{ map => mmm , _}
        implicit val toto5Rule = Rule.gen[JsValue, Toto5]
        success
      }

      "Write" in {
        import js.Writes.{ map => mmm , _}
        implicit val toto5Write = Write.gen[Toto5, JsObject]
        success
      }
    }

    "test case class 1 field seq[Dog]" in {
      import Rules._

      implicit val userRule = Rule.gen[JsValue, User]
      implicit val dogRule = Rule.gen[JsValue, Dog]
      implicit val toto6Rule = Rule.gen[JsValue, Toto6]

      val js = Json.obj("name" -> Json.arr(
        Json.obj(
          "name" -> "medor",
          "master" -> Json.obj("name" -> "toto", "age" -> 45)
        ),
        Json.obj(
          "name" -> "brutus",
          "master" -> Json.obj("name" -> "tata", "age" -> 23)
        )
      ))

      toto6Rule.validate(js) must beEqualTo(Success(
        Toto6(Seq(
          Dog("medor", User(45, "toto")),
          Dog("brutus", User(23, "tata"))
        ))
      ))
    }

    "test case reads in companion object" in {
      From[JsValue, Person](
        To[Person, JsValue](Person("bob", 15))
      ) must beEqualTo(Success(Person("bob", 15)))
    }

    "test case single-field in companion object" in {
      From[JsValue, Person2](
        To[Person2, JsValue](Person2(List("bob", "bobby")))
      ) must beEqualTo(Success(Person2(List("bob", "bobby"))))
    }

  }

}