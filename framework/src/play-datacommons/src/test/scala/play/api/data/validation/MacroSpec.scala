package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

// object MacroSpec extends Specification {

//   case class Contact(
//     firstname: String,
//     lastname: String,
//     company: Option[String],
//     informations: Seq[ContactInformation])

//   case class ContactInformation(
//     label: String,
//     email: Option[String],
//     phones: Seq[String])

//   val contact = Contact("Julien", "Tournay", None, Seq(
//     ContactInformation("Personal", Some("fakecontact@gmail.com"), Seq("01.23.45.67.89", "98.76.54.32.10"))))

//   case class Foo(name: String, i: Int)
//   object Foo {
//     implicit val fooW = To[UrlFormEncoded] { __ =>
//       import Writes._
//       ((__ \ "n").write[String] ~
//        (__ \ "i").write[Int]) (unlift(Foo.unapply _))
//     }
//   }
//   case class Bar(name: String, foo: Foo)

//   "MappingMacros" should {
//     import Writes._

//     "generate Write" in {
//       val w = Write.generate[ContactInformation, UrlFormEncoded]

//       val info = Map(
//         "label" -> Seq("Personal"),
//         "email" -> Seq("fakecontact@gmail.com"),
//         "phones[0]" -> Seq("01.23.45.67.89"),
//         "phones[1]" -> Seq("98.76.54.32.10"))

//       w.writes(contact.informations.head) mustEqual(info)
//     }

//     "find user defined Writes" in {
//       implicit val wc = Write.generate[ContactInformation, UrlFormEncoded]
//       val wu = Write.generate[Contact, UrlFormEncoded]

//       val contactMap = Map(
//         "firstname" -> Seq("Julien"),
//         "lastname" -> Seq("Tournay"),
//         "informations[0].label" -> Seq("Personal"),
//         "informations[0].email" -> Seq("fakecontact@gmail.com"),
//         "informations[0].phones[0]" -> Seq("01.23.45.67.89"),
//         "informations[0].phones[1]" -> Seq("98.76.54.32.10"))

//       wu.writes(contact) mustEqual contactMap
//     }

//     "find implicit writes in companion" in {
//       val wu = Write.generate[Bar, UrlFormEncoded]

//       val m = Map(
//         "name" -> Seq("bar"),
//         "foo.n" -> Seq("foo"),
//         "foo.i" -> Seq("42"))

//       wu.writes(Bar("bar", Foo("foo", 42))) mustEqual(m)
//     }

//   }
// }

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

// case class Person(name: String, age: Int)
// object Person {
//   implicit val personReads = Json.reads[Person]
//   implicit val personWrites = Json.writes[Person]
// }

// import play.api.libs.json._

// case class Person2(names: List[String])

// object Person2{
//   implicit val person2Fmt = Json.format[Person2]
// }

object MacroSpec extends Specification {

  import Writes._
  import Rules._

  "MappingMacros" should {

    "create a Rule[User]" in {
      //object User {def apply(age:Int):User = User(age,"")}
      implicit val userReads = Rule.gen[UrlFormEncoded, User]

      userReads.validate(Map("name" -> Seq("toto"), "age" -> Seq("45"))) must beEqualTo(Success(User(45, "toto")))
    }

    "create a Write[User]" in {
      implicit val userWrites = Write.gen[User, UrlFormEncoded]
      userWrites.writes(User(45, "toto")) must beEqualTo(Map("name" -> Seq("toto"), "age" -> Seq("45")))
    }

    // "create a reads[Dog]" in {
    //   import play.api.libs.json.Json

    //   implicit val userReads = Json.reads[User]
    //   implicit val dogReads = Json.reads[Dog]

    //   Json.fromJson[Dog](
    //     Json.obj(
    //       "name" -> "medor",
    //       "master" -> Json.obj("name" -> "toto", "age" -> 45)
    //     )
    //   ) must beEqualTo(JsSuccess(Dog("medor", User(45, "toto"))))

    // }

    "create a Write[Dog]" in {
      implicit val userWrite = Write.gen[User, UrlFormEncoded]
      implicit val dogWrite = Write.gen[Dog, UrlFormEncoded]

      dogWrite.writes(Dog("medor", User(45, "toto"))) must beEqualTo(
        Map(
          "name" -> Seq("medor"),
          "master.name" -> Seq("toto"),
          "master.age" -> Seq("45")))
    }

    // "create a reads[RecUser]" in {
    //   import play.api.libs.json.Json

    //   implicit val catReads = Json.reads[Cat]
    //   implicit val recUserReads = Json.reads[RecUser]

    //   Json.fromJson[RecUser](
    //     Json.obj(
    //       "name" -> "bob",
    //       "cat" -> Json.obj("name" -> "minou"),
    //       "hobbies" -> Json.arr("bobsleig", "manhunting"),
    //       "friends" -> Json.arr(Json.obj( "name" -> "tom", "hobbies" -> Json.arr(), "friends" -> Json.arr() ))
    //     )
    //   ) must beEqualTo(
    //     JsSuccess(
    //       RecUser(
    //         "bob",
    //         Some(Cat("minou")),
    //         List("bobsleig", "manhunting"),
    //         List(RecUser("tom"))
    //       )
    //     )
    //   )

    // }

    "create a Write[RecUser]" in {

      implicit val catWrite = Write.gen[Cat, UrlFormEncoded]
      catWrite.writes(Cat("minou")) must beEqualTo(Map("name" -> Seq("minou")))

      implicit lazy val recUserWrite: Write[RecUser, UrlFormEncoded] = Write.gen[RecUser, UrlFormEncoded]

      recUserWrite.writes(
        RecUser(
          "bob",
          Some(Cat("minou")),
          Seq("bobsleig", "manhunting"),
          Seq(RecUser("tom"))
        )
      ) must beEqualTo(
        Map(
          "name" -> Seq("bob"),
          "cat.name" -> Seq("minou"),
          "hobbies[0]" -> Seq("bobsleig"),
          "hobbies[1]" -> Seq("manhunting"),
          "friends[0].name" -> Seq("tom"))
      )

    }

    // "create a reads[User1]" in {
    //   import play.api.libs.json.Json

    //   implicit val userReads = Json.reads[User1]

    //   Json.fromJson[User1](
    //     Json.obj(
    //       "name" -> "bob",
    //       "friend" -> Json.obj( "name" -> "tom" )
    //     )
    //   ) must beEqualTo(
    //     JsSuccess(
    //       User1(
    //         "bob",
    //         Some(User1("tom"))
    //       )
    //     )
    //   )
    // }


    "create a writes[User1]" in {
      implicit lazy val userWrites: Write[User1, UrlFormEncoded] = Write.gen[User1, UrlFormEncoded]

      userWrites.writes(
        User1(
          "bob",
          Some(User1("tom")))
      ) must beEqualTo(
        Map(
          "name" -> Seq("bob"),
          "friend.name" -> Seq("tom" )))
    }


    // "create a format[User1]" in {
    //   import play.api.libs.json.Json

    //   implicit val userFormat = Json.format[User1]

    //   Json.fromJson[User1](
    //     Json.obj(
    //       "name" -> "bob",
    //       "friend" -> Json.obj( "name" -> "tom" )
    //     )
    //   ) must beEqualTo(
    //     JsSuccess(
    //       User1(
    //         "bob",
    //         Some(User1("tom"))
    //       )
    //     )
    //   )

    //   Json.toJson(
    //     User1(
    //       "bob",
    //       Some(User1("tom"))
    //     )
    //   ) must beEqualTo(
    //     Json.obj(
    //       "name" -> "bob",
    //       "friend" -> Json.obj( "name" -> "tom" )
    //     )
    //   )

    // }

    // "manage Map[String, User]" in {
    //   import play.api.libs.json.Json

    //   implicit val userReads = Json.reads[UserMap]

    //   Json.fromJson[UserMap](
    //     Json.obj("name" -> "toto", "friends" -> Json.obj("tutu" -> Json.obj("name" -> "tutu", "friends" -> Json.obj())))
    //   ) must beEqualTo(
    //     JsSuccess(UserMap("toto", Map("tutu" -> UserMap("tutu"))))
    //   )
    // }

    // "manage Boxed class" in {
    //   import play.api.libs.functional.syntax._

    //   implicit def idReads[A](implicit rds: Reads[A]): Reads[Id[A]] =
    //     Reads[Id[A]] { js => rds.reads(js).map( Id[A](_) ) }

    //   //val c2Reads1 = Json.reads[C2]

    //   implicit def c1Reads[A](implicit rds: Reads[Id[A]]) = {
    //     (
    //       (__ \ 'id).read(rds) and
    //       (__ \ 'name).read[String]
    //     )( (id, name) => C1[A](id, name) )
    //   }

    //   val js = Json.obj("id" -> 123L, "name" -> "toto")

    //   js.validate(c1Reads[Long]).get must beEqualTo(C1[Long](Id[Long](123L), "toto"))
    // }

    // /** test to validate it doesn't compile if missing implicit
    // "fail if missing " in {
    //   import play.api.libs.json.Json

    //   implicit val userReads = Json.reads[UserFail]

    //   success
    // }*/
    // "test 21 fields" in {
    //   implicit val XReads = Json.reads[X]
    //   implicit val XWrites = Json.writes[X]
    //   implicit val XFormat = Json.format[X]
    //   success
    // }

    // "test inception with overriden object" in {
    //   implicit val programFormat = Json.reads[Program]
    //   success
    // }

    // "test case class 1 field" in {
    //   implicit val totoReads = Json.reads[Toto]
    //   implicit val totoWrites = Json.writes[Toto]
    //   implicit val totoFormat = Json.format[Toto]
    //   success
    // }

    // "test case class 1 field option" in {
    //   implicit val toto2Reads = Json.reads[Toto2]
    //   implicit val toto2Writes = Json.writes[Toto2]
    //   implicit val toto2Format = Json.format[Toto2]
    //   success
    // }

    // "test case class 1 field list" in {
    //   implicit val toto3Reads = Json.reads[Toto3]
    //   implicit val toto3Writes = Json.writes[Toto3]
    //   implicit val toto3Format = Json.format[Toto3]
    //   success
    // }

    // "test case class 1 field set" in {
    //   implicit val toto4Reads = Json.reads[Toto4]
    //   implicit val toto4Writes = Json.writes[Toto4]
    //   implicit val toto4Format = Json.format[Toto4]
    //   success
    // }

    // "test case class 1 field map" in {
    //   implicit val toto5Reads = Json.reads[Toto5]
    //   implicit val toto5Writes = Json.writes[Toto5]
    //   implicit val toto5Format = Json.format[Toto5]
    //   success
    // }

    // "test case class 1 field seq[Dog]" in {
    //   implicit val userFormat = Json.format[User]
    //   implicit val dogFormat = Json.format[Dog]
    //   implicit val toto6Reads = Json.reads[Toto6]
    //   implicit val toto6Writes = Json.writes[Toto6]
    //   implicit val toto6Format = Json.format[Toto6]

    //   val js = Json.obj("name" -> Json.arr(
    //     Json.obj(
    //       "name" -> "medor",
    //       "master" -> Json.obj("name" -> "toto", "age" -> 45)
    //     ),
    //     Json.obj(
    //       "name" -> "brutus",
    //       "master" -> Json.obj("name" -> "tata", "age" -> 23)
    //     )
    //   ))

    //   Json.fromJson[Toto6](js).get must beEqualTo(
    //     Toto6(Seq(
    //       Dog("medor", User(45, "toto")),
    //       Dog("brutus", User(23, "tata"))
    //     ))
    //   )
    // }

    // "test case reads in companion object" in {
    //   Json.fromJson[Person](Json.toJson(Person("bob", 15))).get must beEqualTo(Person("bob", 15))
    // }

    // "test case single-field in companion object" in {
    //   Json.fromJson[Person2](Json.toJson(Person2(List("bob", "bobby")))).get must beEqualTo(Person2(List("bob", "bobby")))
    // }

  }

}
