/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._

case class User(age: Int, name: String)
case class Dog(name: String, master: User)

case class Cat(name: String)

case class RecUser(name: String, cat: Option[Cat] = None, hobbies: List[String] = List(), friends: List[RecUser] = List())

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
  _21: String, _22: String)

case class Program(id: Long, name: String, logoPath: Option[String], logoThumb: Option[String])
object Program {
  def programs = List.empty[Program]
}

case class Person(name: String, age: Int)
object Person {
  implicit val personReads = Json.reads[Person]
  implicit val personWrites = Json.writes[Person]
}

package foreign {
  case class Foreigner(name: String)
}
object ForeignTest {
  implicit val foreignerReads = Json.reads[foreign.Foreigner]
  implicit val foreignerWrites = Json.writes[foreign.Foreigner]
}

import play.api.libs.json._

case class Person2(names: List[String])

case class GenericCaseClass[A](obj: A)
case class GenericCaseClass2[A, B](obj1: A, obj2: B)
case class WrappedGenericInt(int: GenericCaseClass[Int])
case class WrappedGenericIntString(intString: GenericCaseClass2[Int, String])

case class VarArgsOnly(ints: Int*)
case class LastVarArg(name: String, ints: Int*)

object Person2 {
  implicit val person2Fmt = Json.format[Person2]
}

object JsonExtensionSpec extends Specification {

  "JsonExtension" should {
    "create a reads[User]" in {
      import play.api.libs.json.Json

      //object User {def apply(age:Int):User = User(age,"")}
      implicit val userReads = Json.reads[User]

      Json.fromJson[User](Json.obj("name" -> "toto", "age" -> 45)) must beEqualTo(JsSuccess(User(45, "toto")))
    }

    "create a writes[User]" in {
      import play.api.libs.json.Json

      implicit val userWrites = Json.writes[User]

      Json.toJson(User(45, "toto")) must beEqualTo(Json.obj("name" -> "toto", "age" -> 45))
    }

    "create a format[User]" in {
      import play.api.libs.json.Json

      implicit val userFormat = Json.format[User]

      Json.fromJson[User](Json.obj("name" -> "toto", "age" -> 45)) must beEqualTo(JsSuccess(User(45, "toto")))
      Json.toJson(User(45, "toto")) must beEqualTo(Json.obj("name" -> "toto", "age" -> 45))
    }

    "create a reads[Dog]" in {
      import play.api.libs.json.Json

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

      implicit val catReads = Json.reads[Cat]
      implicit val recUserReads = Json.reads[RecUser]

      Json.fromJson[RecUser](
        Json.obj(
          "name" -> "bob",
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj("name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr()))
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
            "friends" -> Json.arr(Json.obj("name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr()))
          )
        )

    }

    "create a format[RecUser]" in {
      import play.api.libs.json.Json

      implicit val catFormat = Json.format[Cat]
      implicit val recUserFormat = Json.format[RecUser]

      Json.fromJson[RecUser](
        Json.obj(
          "name" -> "bob",
          "cat" -> Json.obj("name" -> "minou"),
          "hobbies" -> Json.arr("bobsleig", "manhunting"),
          "friends" -> Json.arr(Json.obj("name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr()))
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
            "friends" -> Json.arr(Json.obj("name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr()))
          )
        )

    }

    "create a reads[User1]" in {
      import play.api.libs.json.Json

      implicit val userReads = Json.reads[User1]

      Json.fromJson[User1](
        Json.obj(
          "name" -> "bob",
          "friend" -> Json.obj("name" -> "tom")
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

      implicit val userWrites = Json.writes[User1]

      Json.toJson(
        User1(
          "bob",
          Some(User1("tom"))
        )
      ) must beEqualTo(
          Json.obj(
            "name" -> "bob",
            "friend" -> Json.obj("name" -> "tom")
          )
        )

    }

    "create a format[User1]" in {
      import play.api.libs.json.Json

      implicit val userFormat = Json.format[User1]

      Json.fromJson[User1](
        Json.obj(
          "name" -> "bob",
          "friend" -> Json.obj("name" -> "tom")
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
            "friend" -> Json.obj("name" -> "tom")
          )
        )

    }

    "create a format[WrappedGenericInt]" in {
      import play.api.libs.json.Json._
      import play.api.libs.functional.syntax._

      implicit def genericFormat[A: Format]: Format[GenericCaseClass[A]] =
        (
          (
            (__ \ "obj").format[A]
          ).inmap
        )(GenericCaseClass[A] _, unlift(GenericCaseClass.unapply[A]))

      implicit val wrappedGenericIntFormat = Json.format[WrappedGenericInt]

      val genericInt = GenericCaseClass(obj = 1)
      val wrapped = WrappedGenericInt(int = genericInt)

      val expectedJsObj = Json.obj(
        "int" -> Json.obj("obj" -> 1)
      )
      Json.toJson(wrapped) must beEqualTo(expectedJsObj)
      Json.fromJson[WrappedGenericInt](expectedJsObj).get must beEqualTo(wrapped)
    }

    "create a format[WrappedGenericIntString]" in {
      import play.api.libs.json.Json._
      import play.api.libs.functional.syntax._

      implicit def genericEntityWrapperFormat[A: Format, B: Format]: Format[GenericCaseClass2[A, B]] =
        (
          (
            (__ \ "obj1").format[A] and
            (__ \ "obj2").format[B]
          )
        )(GenericCaseClass2[A, B] _, unlift(GenericCaseClass2.unapply[A, B]))

      implicit val genericHolderFormat = Json.format[WrappedGenericIntString]

      val genericIntString = GenericCaseClass2(obj1 = 1, obj2 = "hello")
      val genericHolder = WrappedGenericIntString(intString = genericIntString)
      val expectedJsObj = Json.obj(
        "intString" -> Json.obj("obj1" -> 1, "obj2" -> "hello")
      )
      Json.toJson(genericHolder) must beEqualTo(expectedJsObj)
      Json.fromJson[WrappedGenericIntString](expectedJsObj).get must beEqualTo(genericHolder)
    }

    "VarArgsOnly reads, writes, format" should {

      val reads = Json.reads[VarArgsOnly]
      val writes = Json.writes[VarArgsOnly]
      val format = Json.format[VarArgsOnly]

      val obj = VarArgsOnly(1, 2, 3)
      val jsObj = Json.obj("ints" -> Seq(1, 2, 3))

      "formats should be able to read and write" in {
        Json.toJson(obj)(format) must beEqualTo(jsObj)
        jsObj.as[VarArgsOnly](format) must beEqualTo(obj)
      }

      "reads should be able to read valid Json and ignore invalid Json" in {
        jsObj.as[VarArgsOnly](reads) must beEqualTo(obj)
        Json.fromJson[VarArgsOnly](Json.obj("hello" -> "world"))(reads).isError must beTrue
      }

      "writes should be able to spit out valid json" in {
        Json.toJson(obj)(writes) must beEqualTo(jsObj)
      }
    }

    "LastVarArg reads, writes, format" should {

      val reads = Json.reads[LastVarArg]
      val writes = Json.writes[LastVarArg]
      val format = Json.format[LastVarArg]

      val obj = LastVarArg("hello", 1, 2, 3)
      val jsObj = Json.obj("name" -> "hello", "ints" -> Seq(1, 2, 3))

      "formats should be able to read and write" in {
        Json.toJson(obj)(format) must beEqualTo(jsObj)
        jsObj.as[LastVarArg](format) must beEqualTo(obj)
      }

      "reads should be able to read valid Json and ignore invalid Json" in {
        jsObj.as[LastVarArg](reads) must beEqualTo(obj)
        Json.fromJson[LastVarArg](Json.obj("hello" -> "world"))(reads).isError must beTrue
      }

      "writes should be able to spit out valid json" in {
        Json.toJson(obj)(writes) must beEqualTo(jsObj)
      }
    }

    "manage Map[String, User]" in {
      import play.api.libs.json.Json

      implicit val userReads = Json.reads[UserMap]

      Json.fromJson[UserMap](
        Json.obj("name" -> "toto", "friends" -> Json.obj("tutu" -> Json.obj("name" -> "tutu", "friends" -> Json.obj())))
      ) must beEqualTo(
          JsSuccess(UserMap("toto", Map("tutu" -> UserMap("tutu"))))
        )
    }

    "manage Boxed class" in {
      import play.api.libs.functional.syntax._

      implicit def idReads[A](implicit rds: Reads[A]): Reads[Id[A]] =
        Reads[Id[A]] { js => rds.reads(js).map(Id[A](_)) }

      //val c2Reads1 = Json.reads[C2]

      implicit def c1Reads[A](implicit rds: Reads[Id[A]]) = {
        (
          (__ \ 'id).read(rds) and
          (__ \ 'name).read[String]
        )((id, name) => C1[A](id, name))
      }

      val js = Json.obj("id" -> 123L, "name" -> "toto")

      js.validate(c1Reads[Long]).get must beEqualTo(C1[Long](Id[Long](123L), "toto"))
    }

    /**
     * test to validate it doesn't compile if missing implicit
     * "fail if missing " in {
     * import play.api.libs.json.Json
     *
     * implicit val userReads = Json.reads[UserFail]
     *
     * success
     * }
     */
    "test 21 fields" in {
      implicit val XReads = Json.reads[X]
      implicit val XWrites = Json.writes[X]
      implicit val XFormat = Json.format[X]
      success
    }

    "test inception with overriden object" in {
      implicit val programFormat = Json.reads[Program]
      success
    }

    "test case class 1 field" in {
      implicit val totoReads = Json.reads[Toto]
      implicit val totoWrites = Json.writes[Toto]
      implicit val totoFormat = Json.format[Toto]
      success
    }

    "test case class 1 field option" in {
      implicit val toto2Reads = Json.reads[Toto2]
      implicit val toto2Writes = Json.writes[Toto2]
      implicit val toto2Format = Json.format[Toto2]
      success
    }

    "test case class 1 field list" in {
      implicit val toto3Reads = Json.reads[Toto3]
      implicit val toto3Writes = Json.writes[Toto3]
      implicit val toto3Format = Json.format[Toto3]
      success
    }

    "test case class 1 field set" in {
      implicit val toto4Reads = Json.reads[Toto4]
      implicit val toto4Writes = Json.writes[Toto4]
      implicit val toto4Format = Json.format[Toto4]
      success
    }

    "test case class 1 field map" in {
      implicit val toto5Reads = Json.reads[Toto5]
      implicit val toto5Writes = Json.writes[Toto5]
      implicit val toto5Format = Json.format[Toto5]
      success
    }

    "test case class 1 field seq[Dog]" in {
      implicit val userFormat = Json.format[User]
      implicit val dogFormat = Json.format[Dog]
      implicit val toto6Reads = Json.reads[Toto6]
      implicit val toto6Writes = Json.writes[Toto6]
      implicit val toto6Format = Json.format[Toto6]

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

      Json.fromJson[Toto6](js).get must beEqualTo(
        Toto6(Seq(
          Dog("medor", User(45, "toto")),
          Dog("brutus", User(23, "tata"))
        ))
      )
    }

    "test case reads in companion object" in {
      Json.fromJson[Person](Json.toJson(Person("bob", 15))).get must beEqualTo(Person("bob", 15))
    }

    "test case single-field in companion object" in {
      Json.fromJson[Person2](Json.toJson(Person2(List("bob", "bobby")))).get must beEqualTo(Person2(List("bob", "bobby")))
    }

  }

}
