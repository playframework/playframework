/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.util.control.Exception._
import java.text.ParseException
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._

object JsonValidSpec extends Specification {
  "JSON reads" should {
    "validate simple types" in {
      JsString("string").validate[String] must equalTo(JsSuccess("string"))
      JsNumber(5).validate[Int] must equalTo(JsSuccess(5))
      JsNumber(5L).validate[Long] must equalTo(JsSuccess(5L))
      JsNumber(5).validate[Short] must equalTo(JsSuccess(5))
      JsNumber(123.5).validate[Float] must equalTo(JsSuccess(123.5))
      JsNumber(123456789123456.56).validate[Double] must equalTo(JsSuccess(123456789123456.56))
      JsBoolean(true).validate[Boolean] must equalTo(JsSuccess(true))
      JsString("123456789123456.56").validate[BigDecimal] must equalTo(JsSuccess(BigDecimal(123456789123456.56)))
      JsNumber(123456789123456.56).validate[BigDecimal] must equalTo(JsSuccess(BigDecimal(123456789123456.567891234)))
      JsNumber(123456789.56).validate[java.math.BigDecimal] must equalTo(JsSuccess(new java.math.BigDecimal("123456789.56")))
      JsString("123456789123456.56").validate[java.math.BigDecimal] must equalTo(JsSuccess(new java.math.BigDecimal("123456789123456.56")))
    }

    "invalidate wrong simple type conversion" in {
      JsString("string").validate[Long] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber")))))
      JsNumber(5).validate[String] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring")))))
      JsNumber(5.123).validate[Int] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.int")))))
      JsNumber(300).validate[Byte] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.byte")))))
      JsNumber(Long.MaxValue).validate[Int] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.int")))))
      JsBoolean(false).validate[Double] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber")))))
    }

    "validate simple numbered type conversion" in {
      JsNumber(5).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(BigDecimal(5)).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(5.123).validate[BigDecimal] must equalTo(JsSuccess(BigDecimal(5.123)))
    }

    "return JsResult with correct values for isSuccess and isError" in {
      JsString("s").validate[String].isSuccess must beTrue
      JsString("s").validate[String].isError must beFalse
      JsString("s").validate[Long].isSuccess must beFalse
      JsString("s").validate[Long].isError must beTrue
    }

    "validate JsObject to Map" in {
      Json.obj("key1" -> "value1", "key2" -> "value2").validate[Map[String, String]] must equalTo(JsSuccess(Map("key1" -> "value1", "key2" -> "value2")))
      Json.obj("key1" -> 5, "key2" -> 3).validate[Map[String, Int]] must equalTo(JsSuccess(Map("key1" -> 5, "key2" -> 3)))
      Json.obj("key1" -> 5.123, "key2" -> 3.543).validate[Map[String, Float]] must equalTo(JsSuccess(Map("key1" -> 5.123F, "key2" -> 3.543F)))
      Json.obj("key1" -> 5.123, "key2" -> 3.543).validate[Map[String, Double]] must equalTo(JsSuccess(Map("key1" -> 5.123, "key2" -> 3.543)))
    }

    "invalidate JsObject to Map with wrong type conversion" in {
      Json.obj("key1" -> "value1", "key2" -> "value2", "key3" -> "value3").validate[Map[String, Int]] must equalTo(
        JsError(Seq(
          JsPath \ "key1" -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath \ "key2" -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath \ "key3" -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )

      Json.obj("key1" -> "value1", "key2" -> 5, "key3" -> true).validate[Map[String, Int]] must equalTo(
        JsError(Seq(
          JsPath \ "key1" -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath \ "key3" -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )
    }

    "validate JsArray to List" in {
      Json.arr("alpha", "beta", "delta").validate[List[String]] must equalTo(JsSuccess(List("alpha", "beta", "delta")))
      Json.arr(123, 567, 890).validate[List[Int]] must equalTo(JsSuccess(List(123, 567, 890)))
      Json.arr(123.456, 567.123, 890.654).validate[List[Double]] must equalTo(JsSuccess(List(123.456, 567.123, 890.654)))
    }

    "invalidate JsArray to List with wrong type conversion" in {
      Json.arr(123.456, 567.123, 890.654).validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("error.expected.int")),
          JsPath(1) -> Seq(ValidationError("error.expected.int")),
          JsPath(2) -> Seq(ValidationError("error.expected.int"))
        ))
      )
      Json.arr("alpha", "beta", "delta").validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(1) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )

      Json.arr("alpha", 5, true).validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )
    }

    "validate JsArray of stream to List" in {
      JsArray(Stream("alpha", "beta", "delta") map JsString.apply).validate[List[String]] must equalTo(JsSuccess(List("alpha", "beta", "delta")))
    }

    "invalidate JsArray of stream to List with wrong type conversion" in {
      JsArray(Stream(JsNumber(1), JsString("beta"), JsString("delta"), JsNumber(4), JsString("five"))).validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(1) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(4) -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )

      JsArray(Stream(JsString("alpha"), JsNumber(5), JsBoolean(true))).validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("error.expected.jsnumber"))
        ))
      )
    }

    "validate Dates" in {
      val d = new java.util.Date()
      val df = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val dd = df.parse(df.format(d))

      Json.toJson[java.util.Date](dd).validate[java.util.Date] must beEqualTo(JsSuccess(dd))
      JsNumber(dd.getTime).validate[java.util.Date] must beEqualTo(JsSuccess(dd))

      val dj = new org.joda.time.DateTime()
      val dfj = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd")
      val ddj = org.joda.time.DateTime.parse(dfj.print(dj), dfj)

      Json.toJson[org.joda.time.DateTime](ddj).validate[org.joda.time.DateTime] must beEqualTo(JsSuccess(ddj))
      JsNumber(ddj.getMillis).validate[org.joda.time.DateTime] must beEqualTo(JsSuccess(ddj))

      val ldj = org.joda.time.LocalDate.parse(dfj.print(dj), dfj)
      Json.toJson[org.joda.time.LocalDate](ldj).validate[org.joda.time.LocalDate] must beEqualTo(JsSuccess(ldj))

      val ds = new java.sql.Date(dd.getTime())

      val dtfj = org.joda.time.format.DateTimeFormat.forPattern("HH:mm:ss.SSS")
      Json.toJson[java.sql.Date](ds).validate[java.sql.Date] must beEqualTo(JsSuccess(dd))
      JsNumber(dd.getTime).validate[java.sql.Date] must beEqualTo(JsSuccess(dd))

      val ltj = org.joda.time.LocalTime.parse(dtfj.print(dj), dtfj)
      Json.toJson[org.joda.time.LocalTime](ltj).validate[org.joda.time.LocalTime] must beEqualTo(JsSuccess(ltj))

      // very poor test to do really crappy java date APIs
      // TODO ISO8601 test doesn't work on CI platform...
      val c = java.util.Calendar.getInstance()
      c.setTime(new java.util.Date(d.getTime - d.getTime % 1000))
      val js = {
        val fmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        JsString(fmt format c.getTime)
      }
      js.validate[java.util.Date](Reads.IsoDateReads).
        aka("formatted date") must beEqualTo(JsSuccess(c.getTime))
    }

    "validate UUID" in {
      "validate correct UUIDs" in {
        val uuid = java.util.UUID.randomUUID()
        Json.toJson[java.util.UUID](uuid).validate[java.util.UUID] must beEqualTo(JsSuccess(uuid))
      }

      "reject malformed UUIDs" in {
        JsString("bogus string").validate[java.util.UUID].recoverTotal {
          e => "error"
        } must beEqualTo("error")
      }
      "reject well-formed but incorrect UUIDS in strict mode" in {
        JsString("0-0-0-0-0").validate[java.util.UUID](Reads.uuidReader(true)).recoverTotal {
          e => "error"
        } must beEqualTo("error")
      }
    }

    "validate Enums" in {
      object Weekdays extends Enumeration {
        val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
      }
      val json = Json.obj("day1" -> Weekdays.Mon, "day2" -> "tue", "day3" -> 3)

      (json.validate((__ \ "day1").read(Reads.enumNameReads(Weekdays))).asOpt must beSome(Weekdays.Mon)) and
        (json.validate((__ \ "day2").read(Reads.enumNameReads(Weekdays))).asOpt must beNone) and
        (json.validate((__ \ "day3").read(Reads.enumNameReads(Weekdays))).asOpt must beNone)
    }

    "Can reads with nullable" in {
      val json = Json.obj("field" -> JsNull)

      val resultPost = json.validate((__ \ "field").read(Reads.optionWithNull[String]))
      resultPost.get must equalTo(None)
    }
  }

  "JSON JsResult" should {
    "recover from error" in {
      JsNumber(123).validate[String].recover {
        case JsError(e) => "error"
      } must beEqualTo(JsSuccess("error"))

      JsNumber(123).validate[String].recoverTotal {
        e => "error"
      } must beEqualTo("error")

      JsNumber(123).validate[Int].recoverTotal {
        e => 0
      } must beEqualTo(123)
    }
  }

  "JSON caseclass/tuple validation" should {
    case class User(name: String, age: Int)

    "validate simple reads" in {
      JsString("alphabeta").validate[String] must equalTo(JsSuccess("alphabeta"))
    }

    "validate simple constraints" in {
      JsString("alphabeta").validate[String](Reads.minLength(5)) must equalTo(JsSuccess("alphabeta"))
    }

    "test JsPath.create" in {
      val obj = JsPath.createObj(
        JsPath \ "toto" \ "toto1" -> JsString("alpha"),
        JsPath \ "titi" \ "titi1" -> JsString("beta"),
        JsPath \ "titi" \ "titi2" -> JsString("beta2")
      )
      success
    }

    "validate simple case class reads/writes" in {
      val bobby = User("bobby", 54)

      implicit val userReads = {
        import Reads.path._
        (
          at(JsPath \ "name")(Reads.minLength[String](5))
          and
          at(JsPath \ "age")(Reads.min(40))
        )(User)
      }

      implicit val userWrites = {
        import Writes.path._
        (
          at[String](JsPath \ "name")
          and
          at[Int](JsPath \ "age")
        )(unlift(User.unapply))
      }

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "validate simple case class format" in {
      val bobby = User("bobby", 54)

      implicit val userFormats = {
        import Format.path._; import Format.constraints._
        (
          at(JsPath \ "name")(Format(Reads.minLength[String](5), of[String]))
          and
          at(JsPath \ "age")(Format(Reads.min(40), of[Int]))
        )(User, unlift(User.unapply))
      }

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "validate simple case class format" in {
      val bobby = User("bobby", 54)

      implicit val userFormats = {
        import Format.path._; import Format.constraints._
        (
          (__ \ "name").rw(Reads.minLength[String](5), of[String])
          and
          (__ \ "age").rw(Reads.min(40), of[Int])
        ) apply (User, unlift(User.unapply))
      }

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "JsObject tupled reads" in {
      implicit val dataReads: Reads[(String, Int)] = {
        import Reads.path._
        (
          at[String](__ \ "uuid") and
          at[Int](__ \ "nb")
        ).tupled
      }

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      js.validate[(String, Int)] must equalTo(JsSuccess("550e8400-e29b-41d4-a716-446655440000" -> 654))
    }

    "JsObject tupled reads new syntax" in {
      implicit val dataReads: Reads[(String, Int)] = (
        (__ \ "uuid").read[String] and
        (__ \ "nb").read[Int]
      ).tupled

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      js.validate[(String, Int)] must equalTo(JsSuccess("550e8400-e29b-41d4-a716-446655440000" -> 654))
    }

    "JsObject tupled writes" in {
      implicit val dataWrites: Writes[(String, Int)] = (
        (__ \ "uuid").write[String] and
        (__ \ "nb").write[Int]
      ).tupled

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      Json.toJson("550e8400-e29b-41d4-a716-446655440000" -> 654) must equalTo(js)
    }

    "JsObject tupled format" in {
      implicit val dataFormat: Format[(String, Int)] = (
        (__ \ "uuid").format[String] and
        (__ \ "nb").format[Int]
      ).tupled

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      Json.toJson("550e8400-e29b-41d4-a716-446655440000" -> 654) must equalTo(js)
      js.validate[(String, Int)] must equalTo(JsSuccess("550e8400-e29b-41d4-a716-446655440000" -> 654))
    }

    "Format simpler syntax without constraints" in {
      val bobby = User("bobby", 54)

      implicit val userFormats = {
        (
          (__ \ 'name).format[String]
          and
          (__ \ 'age).format[Int]
        )(User, unlift(User.unapply))
      }

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "Format simpler syntax with constraints" in {
      val bobby = User("bobby", 54)

      implicit val userFormat = (
        (__ \ 'name).format(Reads.minLength[String](5))
        and
        (__ \ 'age).format(Reads.min(40))
      )(User, unlift(User.unapply))

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "Compose reads" in {
      val js = Json.obj(
        "field1" -> "alpha",
        "field2" -> 123L,
        "field3" -> Json.obj("field31" -> "beta", "field32" -> 345)
      )
      val reads1 = (__ \ 'field3).json.pick
      val reads2 = ((__ \ 'field32).read[Int] and (__ \ 'field31).read[String]).tupled

      js.validate(reads1 andThen reads2).get must beEqualTo(345 -> "beta")
    }

    "Apply min/max correctly on any numeric type" in {
      case class Numbers(i: Int, l: Long, f: Float, d: Double, bd: BigDecimal)

      implicit val numbersFormat = (
        (__ \ 'i).format[Int](Reads.min(5) andKeep Reads.max(100)) and
        (__ \ 'l).format[Long](Reads.min(5L) andKeep Reads.max(100L)) and
        (__ \ 'f).format[Float](Reads.min(13.0F) andKeep Reads.max(14.0F)) and
        (__ \ 'd).format[Double](Reads.min(0.1) andKeep Reads.max(1.0)) and
        (__ \ 'bd).format[BigDecimal](Reads.min(BigDecimal(5)) andKeep Reads.max(BigDecimal(100)))
      )(Numbers.apply _, unlift(Numbers.unapply))

      val ok = Numbers(42, 55L, 13.5F, 0.3, BigDecimal(33.5))
      val fail = Numbers(42, 55L, 10.5F, 1.3, BigDecimal(33.5))
      val jsOk = Json.toJson(ok)
      val jsFail = Json.toJson(fail)

      jsOk.validate[Numbers] must equalTo(JsSuccess(ok))
      jsFail.validate[Numbers] must equalTo(
        JsError((__ \ 'f), ValidationError("error.min", 13.0F)) ++
          JsError((__ \ 'd), ValidationError("error.max", 1.0))
      )
    }

  }

  "JSON generators" should {
    "Build JSON from JSON Reads" in {
      import Reads._
      val js0 = Json.obj(
        "key1" -> "value1",
        "key2" -> Json.obj(
          "key21" -> 123,
          "key23" -> true,
          "key24" -> "blibli"
        ),
        "key3" -> "alpha"
      )

      val js = Json.obj(
        "key1" -> "value1",
        "key2" -> Json.obj(
          "key21" -> 123,
          "key22" -> Json.obj("key222" -> "blabla"),
          "key23" -> true,
          "key24" -> "blibli"
        ),
        "key3" -> Json.arr("alpha", "beta", "gamma")
      )

      val dt = (new java.util.Date).getTime()
      def func = { JsNumber(dt + 100) }

      val jsonTransformer = (
        (__ \ "key1").json.pickBranch and
        (__ \ "key2").json.pickBranch(
          (
            (__ \ "key22").json.update((__ \ "key222").json.pick) and
            (__ \ "key233").json.copyFrom((__ \ "key23").json.pick)
          ).reduce
        ) and
          (__ \ "key3").json.pickBranch[JsArray](pure(Json.arr("delta"))) and
          (__ \ "key4").json.put(
            Json.obj(
              "key41" -> 345,
              "key42" -> "alpha",
              "key43" -> func
            )
          )
      ).reduce

      val res = Json.obj(
        "key1" -> "value1",
        "key2" -> Json.obj(
          "key21" -> 123,
          "key22" -> "blabla",
          "key23" -> true,
          "key24" -> "blibli",
          "key233" -> true
        ),
        "key3" -> Json.arr("delta"),
        "key4" -> Json.obj("key41" -> 345, "key42" -> "alpha", "key43" -> func)
      )

      js0.validate(jsonTransformer) must beEqualTo(
        //JsError( (__ \ 'key3), "error.expected.jsarray" ) ++
        JsError((__ \ 'key2 \ 'key22), "error.path.missing")
      )

      js.validate(jsonTransformer) must beEqualTo(JsSuccess(res))
    }

  }

  "JSON Reads" should {
    "manage nullable/option" in {
      case class User(name: String, email: String, phone: Option[String])

      implicit val UserReads = (
        (__ \ 'name).read[String] and
        (__ \ 'coords \ 'email).read(Reads.email) and
        (__ \ 'coords \ 'phone).readNullable(Reads.minLength[String](8))
      )(User)

      Json.obj(
        "name" -> "john",
        "coords" -> Json.obj(
          "email" -> "john@xxx.yyy",
          "phone" -> "0123456789"
        )
      ).validate[User] must beEqualTo(
          JsSuccess(User("john", "john@xxx.yyy", Some("0123456789")))
        )

      Json.obj(
        "name" -> "john",
        "coords" -> Json.obj(
          "email" -> "john@xxx.yyy",
          "phone2" -> "0123456789"
        )
      ).validate[User] must beEqualTo(
          JsSuccess(User("john", "john@xxx.yyy", None))
        )

      Json.obj(
        "name" -> "john",
        "coords" -> Json.obj(
          "email" -> "john@xxx.yyy"
        )
      ).validate[User] must beEqualTo(
          JsSuccess(User("john", "john@xxx.yyy", None))
        )

      Json.obj(
        "name" -> "john",
        "coords" -> Json.obj(
          "email" -> "john@xxx.yyy",
          "phone" -> JsNull
        )
      ).validate[User] must beEqualTo(
          JsSuccess(User("john", "john@xxx.yyy", None))
        )

      Json.obj(
        "name" -> "john",
        "coords2" -> Json.obj(
          "email" -> "john@xxx.yyy",
          "phone" -> "0123456789"
        )
      ).validate[User] must beEqualTo(
          JsError(Seq(
            __ \ 'coords \ 'phone -> Seq(ValidationError("error.path.missing")),
            __ \ 'coords \ 'email -> Seq(ValidationError("error.path.missing"))
          ))
        )
    }

    "report correct path for validation errors" in {
      case class User(email: String, phone: Option[String])

      implicit val UserReads = (
        (__ \ 'email).read(Reads.email) and
        (__ \ 'phone).readNullable(Reads.minLength[String](8))
      )(User)

      Json.obj("email" -> "john").validate[User] must beEqualTo(JsError(__ \ "email", ValidationError("error.email")))
      Json.obj("email" -> "john.doe@blibli.com", "phone" -> "4").validate[User] must beEqualTo(JsError(__ \ "phone", ValidationError("error.minLength", 8)))
    }

    "mix reads constraints" in {
      case class User(id: Long, email: String, age: Int)

      implicit val UserReads = (
        (__ \ 'id).read[Long] and
        (__ \ 'email).read(Reads.email andKeep Reads.minLength[String](5)) and
        (__ \ 'age).read(Reads.max(55) or Reads.min(65))
      )(User)

      Json.obj("id" -> 123L, "email" -> "john.doe@blibli.com", "age" -> 50).validate[User] must beEqualTo(JsSuccess(User(123L, "john.doe@blibli.com", 50)))
      Json.obj("id" -> 123L, "email" -> "john.doe@blibli.com", "age" -> 60).validate[User] must beEqualTo(JsError((__ \ 'age), ValidationError("error.max", 55)) ++ JsError((__ \ 'age), ValidationError("error.min", 65)))
      Json.obj("id" -> 123L, "email" -> "john.doe", "age" -> 60).validate[User] must beEqualTo(JsError((__ \ 'email), ValidationError("error.email")) ++ JsError((__ \ 'age), ValidationError("error.max", 55)) ++ JsError((__ \ 'age), ValidationError("error.min", 65)))
    }

    "verifyingIf reads" in {
      implicit val TupleReads: Reads[(String, JsObject)] = (
        (__ \ 'type).read[String] and
        (__ \ 'data).read(
          Reads.verifyingIf[JsObject] { case JsObject(fields) => !fields.isEmpty }(
            ((__ \ "title").read[String] and
              (__ \ "created").read[java.util.Date]).tupled
          )
        )
      ).tupled

      val d = (new java.util.Date()).getTime()
      Json.obj("type" -> "coucou", "data" -> Json.obj()).validate(TupleReads) must beEqualTo(JsSuccess("coucou" -> Json.obj()))
      Json.obj("type" -> "coucou", "data" -> Json.obj("title" -> "blabla", "created" -> d)).validate(TupleReads) must beEqualTo(JsSuccess("coucou" -> Json.obj("title" -> "blabla", "created" -> d)))
      Json.obj("type" -> "coucou", "data" -> Json.obj("title" -> "blabla")).validate(TupleReads) must beEqualTo(JsError(__ \ "data" \ "created", "error.path.missing"))
    }

    "recursive reads" in {
      case class User(id: Long, name: String, friend: Option[User] = None)

      implicit lazy val UserReads: Reads[User] = (
        (__ \ 'id).read[Long] and
        (__ \ 'name).read[String] and
        (__ \ 'friend).lazyReadNullable(UserReads)
      )(User)

      val js = Json.obj(
        "id" -> 123L,
        "name" -> "bob",
        "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> JsNull)
      )

      js.validate[User] must beEqualTo(JsSuccess(User(123L, "bob", Some(User(124L, "john", None)))))

      val js2 = Json.obj(
        "id" -> 123L,
        "name" -> "bob",
        "friend" -> Json.obj("id" -> 124L, "name" -> "john")
      )

      js2.validate[User] must beEqualTo(JsSuccess(User(123L, "bob", Some(User(124L, "john", None)))))

      success
    }

    "recursive writes" in {
      import Writes.constraints._

      case class User(id: Long, name: String, friend: Option[User] = None)

      implicit lazy val UserWrites: Writes[User] = (
        (__ \ 'id).write[Long] and
        (__ \ 'name).write[String] and
        (__ \ 'friend).lazyWriteNullable(UserWrites)
      )(unlift(User.unapply))

      val js = Json.obj(
        "id" -> 123L,
        "name" -> "bob",
        "friend" -> Json.obj("id" -> 124L, "name" -> "john")
      )

      Json.toJson(User(123L, "bob", Some(User(124L, "john", None)))) must beEqualTo(js)
      success
    }

    "recursive formats" in {
      import Format.constraints._

      case class User(id: Long, name: String, friend: Option[User] = None)

      implicit lazy val UserFormats: Format[User] = (
        (__ \ 'id).format[Long] and
        (__ \ 'name).format[String] and
        (__ \ 'friend).lazyFormatNullable(UserFormats)
      )(User, unlift(User.unapply))

      val js = Json.obj(
        "id" -> 123L,
        "name" -> "bob",
        "friend" -> Json.obj("id" -> 124L, "name" -> "john")
      )

      js.validate[User] must beEqualTo(JsSuccess(User(123L, "bob", Some(User(124L, "john", None)))))
      Json.toJson(User(123L, "bob", Some(User(124L, "john", None)))) must beEqualTo(js)
      success
    }

    "lots of fields to read" in {
      val myReads = (
        (__ \ 'field1).read[String] and
        (__ \ 'field2).read[Long] and
        (__ \ 'field3).read[Float] and
        (__ \ 'field4).read[Boolean] and
        (__ \ 'field5).read[List[String]] and
        (__ \ 'field6).read[String] and
        (__ \ 'field7).read[String] and
        (__ \ 'field8).read[String] and
        (__ \ 'field9).read[String] and
        (__ \ 'field10).read[String] and
        (__ \ 'field11).read[String] and
        (__ \ 'field12).read[String]
      ).tupled

      Json.obj(
        "field1" -> "val1",
        "field2" -> 123L,
        "field3" -> 123.456F,
        "field4" -> true,
        "field5" -> Json.arr("alpha", "beta"),
        "field6" -> "val6",
        "field7" -> "val7",
        "field8" -> "val8",
        "field9" -> "val9",
        "field10" -> "val10",
        "field11" -> "val11",
        "field12" -> "val12"
      ).validate(myReads) must beEqualTo(
          JsSuccess(("val1", 123L, 123.456F, true, List("alpha", "beta"), "val6", "val7", "val8", "val9", "val10", "val11", "val12"))
        )
    }

    "single field case class" in {
      case class Test(field: String)
      val myFormat = (__ \ 'field).format[String].inmap(Test, unlift(Test.unapply))

      myFormat.reads(Json.obj("field" -> "blabla")) must beEqualTo(JsSuccess(Test("blabla"), __ \ 'field))
      myFormat.reads(Json.obj()) must beEqualTo(JsError(__ \ 'field, "error.path.missing"))
      myFormat.writes(Test("blabla")) must beEqualTo(Json.obj("field" -> "blabla"))
    }

    "reduce Reads[JsObject]" in {
      import Reads._

      val myReads: Reads[JsObject] = (
        (__ \ 'field1).json.pickBranch and
        (__ \ 'field2).json.pickBranch
      ).reduce

      val js0 = Json.obj("field1" -> "alpha")
      val js = js0 ++ Json.obj("field2" -> Json.obj("field21" -> 123, "field22" -> true))
      val js2 = js ++ Json.obj("field3" -> "beta")
      js.validate(myReads) must beEqualTo(JsSuccess(js))
      js2.validate(myReads) must beEqualTo(JsSuccess(js))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "error.path.missing"))
    }

    "reduce Reads[JsArray]" in {
      import Reads._

      val myReads: Reads[JsArray] = (
        (__ \ 'field1).json.pick[JsString] and
        (__ \ 'field2).json.pick[JsNumber] and
        (__ \ 'field3).json.pick[JsBoolean]
      ).reduce[JsValue, JsArray]

      val js0 = Json.obj("field1" -> "alpha")
      val js = js0 ++ Json.obj("field2" -> 123L, "field3" -> false)
      val js2 = js ++ Json.obj("field4" -> false)
      js.validate(myReads) must beEqualTo(JsSuccess(Json.arr("alpha", 123L, false)))
      js2.validate(myReads) must beEqualTo(JsSuccess(Json.arr("alpha", 123L, false)))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "error.path.missing") ++ JsError(__ \ 'field3, "error.path.missing"))
    }

    "reduce Reads[JsArray] no type" in {
      import Reads._

      val myReads: Reads[JsArray] = (
        (__ \ 'field1).json.pick and
        (__ \ 'field2).json.pick and
        (__ \ 'field3).json.pick
      ).reduce

      val js0 = Json.obj("field1" -> "alpha")
      val js = js0 ++ Json.obj("field2" -> 123L, "field3" -> false)
      val js2 = js ++ Json.obj("field4" -> false)
      js.validate(myReads) must beEqualTo(JsSuccess(Json.arr("alpha", 123L, false)))
      js2.validate(myReads) must beEqualTo(JsSuccess(Json.arr("alpha", 123L, false)))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "error.path.missing") ++ JsError(__ \ 'field3, "error.path.missing"))
    }

    "serialize JsError to json" in {
      val jserr = JsError(Seq(
        (__ \ 'field1 \ 'field11) -> Seq(
          ValidationError(Seq("msg1.msg11", "msg1.msg12"), "arg11", 123L, 123.456F),
          ValidationError("msg2.msg21.msg22", 456, 123.456, true, 123)
        ),
        (__ \ 'field2 \ 'field21) -> Seq(
          ValidationError("msg1.msg21", "arg1", Json.obj("test" -> "test2")),
          ValidationError("msg2", "arg1", "arg2")
        )
      ))

      val json = Json.obj(
        "obj.field1.field11" -> Json.arr(
          Json.obj(
            "msg" -> Json.arr("msg1.msg11", "msg1.msg12"),
            "args" -> Json.arr("arg11", 123, 123.456F)
          ),
          Json.obj(
            "msg" -> Json.arr("msg2.msg21.msg22"),
            "args" -> Json.arr(456, 123.456, true, 123)
          )
        ),
        "obj.field2.field21" -> Json.arr(
          Json.obj(
            "msg" -> Json.arr("msg1.msg21"),
            "args" -> Json.arr("arg1", Json.obj("test" -> "test2"))
          ),
          Json.obj(
            "msg" -> Json.arr("msg2"),
            "args" -> Json.arr("arg1", "arg2")
          )
        )
      )

      JsError.toJson(jserr) should beEqualTo(json)
    }

    "prune json" in {
      import Reads._

      val js = Json.obj(
        "field1" -> "alpha",
        "field2" -> Json.obj("field21" -> 123, "field22" -> true, "field23" -> "blabla"),
        "field3" -> "beta"
      )

      val res = Json.obj(
        "field1" -> "alpha",
        "field2" -> Json.obj("field22" -> true),
        "field3" -> "beta"
      )

      val myReads: Reads[JsObject] = (
        (__ \ 'field1).json.pickBranch and
        (__ \ 'field2).json.pickBranch(
          (__ \ 'field21).json.prune andThen (__ \ 'field23).json.prune
        ) and
          (__ \ 'field3).json.pickBranch
      ).reduce

      js.validate(myReads) must beEqualTo(JsSuccess(res))
    }
  }

  "JSON Writes" should {
    "manage option" in {
      import Writes._

      case class User(email: String, phone: Option[String])

      implicit val UserWrites = (
        (__ \ 'email).write[String] and
        (__ \ 'phone).writeNullable[String]
      )(unlift(User.unapply))

      Json.toJson(User("john.doe@blibli.com", None)) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com"))
      Json.toJson(User("john.doe@blibli.com", Some("12345678"))) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com", "phone" -> "12345678"))
    }

    "join" in {
      val joinWrites = (
        (__ \ 'alpha).write[JsString] and
        (__ \ 'beta).write[JsValue]
      ).join

      joinWrites.writes(JsString("toto")) must beEqualTo(Json.obj("alpha" -> "toto", "beta" -> "toto"))

      val joinWrites2 = (
        (__ \ 'alpha).write[JsString] and
        (__ \ 'beta).write[JsValue] and
        (__ \ 'gamma).write[JsString] and
        (__ \ 'delta).write[JsValue]
      ).join

      joinWrites2.writes(JsString("toto")) must beEqualTo(Json.obj("alpha" -> "toto", "beta" -> "toto", "gamma" -> "toto", "delta" -> "toto"))

    }
  }

  "JSON Format" should {
    "manage option" in {
      import Reads._
      import Writes._

      case class User(email: String, phone: Option[String])

      implicit val UserFormat = (
        (__ \ 'email).format(email) and
        (__ \ 'phone).formatNullable(Format(minLength[String](8), Writes.of[String]))
      )(User, unlift(User.unapply))

      Json.obj("email" -> "john").validate[User] must beEqualTo(JsError(__ \ "email", ValidationError("error.email")))
      Json.obj("email" -> "john.doe@blibli.com", "phone" -> "4").validate[User] must beEqualTo(JsError(__ \ "phone", ValidationError("error.minLength", 8)))
      Json.obj("email" -> "john.doe@blibli.com", "phone" -> "12345678").validate[User] must beEqualTo(JsSuccess(User("john.doe@blibli.com", Some("12345678"))))
      Json.obj("email" -> "john.doe@blibli.com").validate[User] must beEqualTo(JsSuccess(User("john.doe@blibli.com", None)))

      Json.toJson(User("john.doe@blibli.com", None)) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com"))
      Json.toJson(User("john.doe@blibli.com", Some("12345678"))) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com", "phone" -> "12345678"))
    }
  }

  "JsResult" should {

    "be usable in for-comprehensions" in {
      val res = JsSuccess("foo")
      val x = for {
        s <- res
        if s.size < 5
      } yield 42
      x must equalTo(JsSuccess(42))
    }

    "be a functor" in {
      "JsSuccess" in {
        val res1: JsResult[String] = JsSuccess("foo", JsPath(List(KeyPathNode("bar"))))
        res1.map(identity) must equalTo(res1)
      }

      "JsError" in {
        val res2: JsResult[String] = JsError(Seq(JsPath(List(KeyPathNode("bar"))) -> Seq(ValidationError("baz.bah"))))
        res2.map(identity) must equalTo(res2)
      }
    }

    "have filtering methods that allow users to customize the error" in {
      val res: JsResult[String] = JsSuccess("foo")
      val error = JsError(__ \ "bar", "There is a problem")
      res.filter(error)(_ != "foo") must equalTo(error)
      res.filter(error)(_ == "foo") must equalTo(res)
      res.filterNot(error)(_ == "foo") must equalTo(error)
      res.filterNot(error)(_ != "foo") must equalTo(res)
    }

  }
}
