package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.util.control.Exception._
import java.text.ParseException
import play.api.data.validation.ValidationError
import Reads.constraints._
import play.api.libs.functional.syntax._


object JsonValidSpec extends Specification {
  "JSON reads" should {
    "validate simple types" in {
      JsString("string").validate[String] must equalTo(JsSuccess("string"))
      JsNumber(5).validate[Int] must equalTo(JsSuccess(5))
      JsNumber(5L).validate[Long] must equalTo(JsSuccess(5L))
      JsNumber(5).validate[Short] must equalTo(JsSuccess(5))
      JsNumber(123.5).validate[Float] must equalTo(JsSuccess(123.5))
      JsNumber(123456789123456.567891234).validate[Double] must equalTo(JsSuccess(123456789123456.567891234))
      JsBoolean(true).validate[Boolean] must equalTo(JsSuccess(true))
    }

    "invalidate wrong simple type conversion" in {
      JsString("string").validate[Long] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))))
      JsNumber(5).validate[String] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring")))))
      JsBoolean(false).validate[Double] must equalTo(JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))))
    }

    "validate simple numbered type conversion" in {
      JsNumber(5).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(5.123).validate[Int] must equalTo(JsSuccess(5))
      JsNumber(BigDecimal(5)).validate[Double] must equalTo(JsSuccess(5.0))
      JsNumber(5.123).validate[BigDecimal] must equalTo(JsSuccess(BigDecimal(5.123)))
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
          JsPath \ "key1" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \ "key2" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \ "key3" -> Seq(ValidationError("validate.error.expected.jsnumber"))
        ))
      )

      Json.obj("key1" -> "value1", "key2" -> 5, "key3" -> true).validate[Map[String, Int]] must equalTo(
        JsError(Seq(
          JsPath \ "key1" -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath \  "key3" -> Seq(ValidationError("validate.error.expected.jsnumber"))
        ))
      )
    }

    "validate JsArray to List" in {
      Json.arr("alpha", "beta", "delta").validate[List[String]] must equalTo(JsSuccess(List("alpha", "beta", "delta")))
      Json.arr(123, 567, 890).validate[List[Int]] must equalTo(JsSuccess(List(123, 567, 890)))
      Json.arr(123.456, 567.123, 890.654).validate[List[Int]] must equalTo(JsSuccess(List(123, 567, 890)))
      Json.arr(123.456, 567.123, 890.654).validate[List[Double]] must equalTo(JsSuccess(List(123.456, 567.123, 890.654)))
    }

    "invalidate JsArray to List with wrong type conversion" in {
      Json.arr("alpha", "beta", "delta").validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(1) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("validate.error.expected.jsnumber"))
        ))
      )

      Json.arr("alpha", 5, true).validate[List[Int]] must equalTo(
        JsError(Seq(
          JsPath(0) -> Seq(ValidationError("validate.error.expected.jsnumber")),
          JsPath(2) -> Seq(ValidationError("validate.error.expected.jsnumber"))
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

      val ds = new java.sql.Date(dd.getTime())

      Json.toJson[java.sql.Date](ds).validate[java.sql.Date] must beEqualTo(JsSuccess(dd))
      JsNumber(dd.getTime).validate[java.sql.Date] must beEqualTo(JsSuccess(dd))

      // very poor test to do really crappy java date APIs
      // TODO ISO8601 test doesn't work on CI platform...
      /*val c = java.util.Calendar.getInstance()
      c.setTime(new java.util.Date(d.getTime - d.getTime % 1000))
      val tz = c.getTimeZone().getOffset(c.getTime.getTime).toInt / 3600000
      val js = JsString(
        "%04d-%02d-%02dT%02d:%02d:%02d%s%02d:00".format(
          c.get(java.util.Calendar.YEAR), 
          c.get(java.util.Calendar.MONTH) + 1, 
          c.get(java.util.Calendar.DAY_OF_MONTH), 
          c.get(java.util.Calendar.HOUR_OF_DAY), 
          c.get(java.util.Calendar.MINUTE), 
          c.get(java.util.Calendar.SECOND),
          if(tz>0) "+" else "-",
          tz
        )
      )
      js.validate[java.util.Date](Reads.IsoDateReads) must beEqualTo(JsSuccess(c.getTime))*/
    }

  }

  "JSON caseclass/tuple validation" should {
    case class User(name: String, age: Int)

    /*implicit val UserFormat = new Format[User]{
      def reads(json: JsValue): JsResult[User] = (at[String](JsPath \ "name") ~> minLength[String](5)).reads(json).map( User(_) )
      def writes(user: User): JsValue = Json.obj("name" -> user.name)
    }*/


    /*implicit val UserFormat = JsMapper(
      at[String](JsPath \ "name")(minLength(5)))
      ~ at[Int](JsPath \ "age")
    )(User.apply)(User.unapply)*/

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

      implicit val userReads = { import Reads.path._
      (
        at(JsPath \ "name")(minLength[String](5)) 
        and 
        at(JsPath \ "age")(min(40))
      )(User) }

      implicit val userWrites = { import Writes.path._
      (
        at[String](JsPath \ "name")
        and 
        at[Int](JsPath \ "age")
      )(unlift(User.unapply)) }

      val js = Json.toJson(bobby)
      
      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "validate simple case class format" in {
      val bobby = User("bobby", 54)

      implicit val userFormats = { import Format.path._; import Format.constraints._
      (
        at(JsPath \ "name")(Format(minLength[String](5), of[String]))
        and 
        at(JsPath \ "age")(Format(min(40), of[Int]))
      )(User, unlift(User.unapply)) }

      val js = Json.toJson(bobby)
      
      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "validate simple case class format" in {
      val bobby = User("bobby", 54)

      implicit val userFormats = { import Format.path._; import Format.constraints._
      (
        (__ \ "name").rw(minLength[String](5), of[String])
        and 
        (__ \ "age").rw(min(40), of[Int])
      ) apply (User, unlift(User.unapply))
     }

      val js = Json.toJson(bobby)
      
      js.validate[User] must equalTo(JsSuccess(bobby))
    }
    
    "JsObject tupled reads" in {
      implicit val dataReads: Reads[(String, Int)] = { import Reads.path._
        (
          at[String]( __ \ "uuid" ) and 
          at[Int]( __ \ "nb" )
        ) tupled
      }

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      js.validate[(String, Int)] must equalTo(JsSuccess("550e8400-e29b-41d4-a716-446655440000" -> 654))
    }

    "JsObject tupled reads new syntax" in {
      implicit val dataReads: Reads[(String, Int)] = (
        ( __ \ "uuid" ).read[String] and 
        ( __ \ "nb" ).read[Int]
      ) tupled

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      js.validate[(String, Int)] must equalTo(JsSuccess("550e8400-e29b-41d4-a716-446655440000" -> 654))
    }

    "JsObject tupled writes" in {
      implicit val dataWrites: Writes[(String, Int)] = (
        ( __ \ "uuid" ).write[String] and
        ( __ \ "nb" ).write[Int]
      ) tupled

      val js = Json.obj(
        "uuid" -> "550e8400-e29b-41d4-a716-446655440000",
        "nb" -> 654
      )

      Json.toJson("550e8400-e29b-41d4-a716-446655440000" -> 654) must equalTo(js)
    }

    "JsObject tupled format" in {
      implicit val dataFormat: Format[(String, Int)] = (
        ( __ \ "uuid" ).format[String] and
        ( __ \ "nb" ).format[Int]
      ) tupled

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
      )(User, unlift(User.unapply)) }

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "Format simpler syntax with constraints" in {
      val bobby = User("bobby", 54)

      implicit val userFormat = (
        (__ \ 'name).format(minLength[String](5)) 
        and 
        (__ \ 'age).format(min(40))
      )(User, unlift(User.unapply))

      val js = Json.toJson(bobby)

      js.validate[User] must equalTo(JsSuccess(bobby))
    }

    "Compose reads" in {
      val js = Json.obj(
        "field1" -> "alpha",
        "field2" -> 123L,
        "field3" -> Json.obj("field31" -> "beta", "field32"-> 345)
      )
      val reads1 = (__ \ 'field3).json.pick
      val reads2 = (__ \ 'field32).read[Int] and (__ \ 'field31).read[String] tupled

      js.validate(reads1 andThen reads2).get must beEqualTo(345 -> "beta")
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
            (__ \ "key22").json.update( (__ \ "key222").json.pick ) and 
            (__ \ "key233").json.copyFrom( (__ \ "key23").json.pick )
          ) reduce
        ) and
        (__ \ "key3").json.pickBranch[JsArray]( pure(Json.arr("delta")) ) and
        (__ \ "key4").json.put(
          Json.obj( 
            "key41" -> 345,
            "key42" -> "alpha",
            "key43" -> func
          )
        )
      ) reduce

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
        //JsError( (__ \ 'key3), "validate.error.expected.jsarray" ) ++
        JsError( (__ \ 'key2 \ 'key22), "validate.error.missing-path" )
      )

      js.validate(jsonTransformer) must beEqualTo(JsSuccess(res))
    }

  }

  "JSON Reads" should {
    "report correct path for validation errors" in {
      case class User(email: String, phone: Option[String])

      implicit val UserReads = (
        (__ \ 'email).read(email) and
        (__ \ 'phone).readOpt(minLength[String](8))
      )(User)

      Json.obj("email" -> "john").validate[User] must beEqualTo(JsError(__ \ "email", ValidationError("validate.error.email")))
      Json.obj("email" -> "john.doe@blibli.com", "phone" -> "4").validate[User] must beEqualTo(JsError(__ \ "phone", ValidationError("validate.error.minlength", 8)))
    }

    "mix reads constraints" in {
      case class User(id: Long, email: String, age: Int)

      implicit val UserReads = (
        (__ \ 'id).read[Long] and
        (__ \ 'email).read( email andKeep minLength[String](5) ) and
        (__ \ 'age).read( max(55) or min(65) )
      )(User)

      Json.obj( "id" -> 123L, "email" -> "john.doe@blibli.com", "age" -> 50).validate[User] must beEqualTo(JsSuccess(User(123L, "john.doe@blibli.com", 50)))
      Json.obj( "id" -> 123L, "email" -> "john.doe@blibli.com", "age" -> 60).validate[User] must beEqualTo(JsError((__ \ 'age), ValidationError("validate.error.max", 55)) ++ JsError((__ \ 'age), ValidationError("validate.error.min", 65)))
      Json.obj( "id" -> 123L, "email" -> "john.doe", "age" -> 60).validate[User] must beEqualTo(JsError((__ \ 'email), ValidationError("validate.error.email")) ++ JsError((__ \ 'age), ValidationError("validate.error.max", 55)) ++ JsError((__ \ 'age), ValidationError("validate.error.min", 65)))
    }

    "verifyingIf reads" in {
      implicit val TupleReads: Reads[(String, JsObject)] = 
        (__ \ 'type).read[String] and
        (__ \ 'data).read( 
          verifyingIf[JsObject]{ case JsObject(fields) => !fields.isEmpty }(
            (__ \ "title").read[String] and 
            (__ \ "created").read[java.util.Date] tupled
          )
        ) tupled

      val d = (new java.util.Date()).getTime()
      Json.obj("type" -> "coucou", "data" -> Json.obj()).validate(TupleReads) must beEqualTo(JsSuccess("coucou" -> Json.obj()))
      Json.obj("type" -> "coucou", "data" -> Json.obj( "title" -> "blabla", "created" -> d)).validate(TupleReads) must beEqualTo(JsSuccess("coucou" -> Json.obj( "title" -> "blabla", "created" -> d)))
      Json.obj("type" -> "coucou", "data" -> Json.obj( "title" -> "blabla")).validate(TupleReads) must beEqualTo(JsError( __ \ "data" \ "created", "validate.error.missing-path"))
    }

    "recursive reads" in {
      case class User(id: Long, name: String, friend: Option[User] = None)

      implicit lazy val UserReads: Reads[User] = (
        (__ \ 'id).read[Long] and
        (__ \ 'name).read[String] and
        (__ \ 'friend).lazyRead[Option[User]](optional(UserReads))
      )(User)

      val js = Json.obj(
        "id" -> 123L, 
        "name" -> "bob", 
        "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> JsNull)
      )

      js.validate[User] must beEqualTo(JsSuccess(User(123L, "bob", Some(User(124L, "john", None)))))
      success
    }

    "recursive writes" in {
      import Writes.constraints._

      case class User(id: Long, name: String, friend: Option[User] = None)

      implicit lazy val UserWrites: Writes[User] = (
        (__ \ 'id).write[Long] and
        (__ \ 'name).write[String] and
        (__ \ 'friend).lazyWrite[Option[User]](optional(UserWrites))
      )(unlift(User.unapply))

      val js = Json.obj(
        "id" -> 123L, 
        "name" -> "bob", 
        "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> Json.obj())
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
        (__ \ 'friend).lazyFormat[Option[User]](optional(UserFormats))
      )(User, unlift(User.unapply))

      val js = Json.obj(
        "id" -> 123L, 
        "name" -> "bob", 
        "friend" -> Json.obj("id" -> 124L, "name" -> "john", "friend" -> Json.obj())
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
      ) tupled

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
        JsSuccess(( "val1", 123L, 123.456F, true, List("alpha", "beta"), "val6", "val7", "val8", "val9", "val10", "val11", "val12"))
      )
    }

    "single field case class" in {
      case class Test(field: String)
      val myFormat = (__ \ 'field).format[String].inmap(Test, unlift(Test.unapply))

      myFormat.reads(Json.obj("field" -> "blabla")) must beEqualTo(JsSuccess(Test("blabla"), __ \ 'field))
      myFormat.reads(Json.obj()) must beEqualTo(JsError( __ \ 'field, "validate.error.missing-path" ) )
      myFormat.writes(Test("blabla")) must beEqualTo(Json.obj("field" -> "blabla"))
    }

    "reduce Reads[JsObject]" in {
      import Reads._

      val myReads: Reads[JsObject] = (
        (__ \ 'field1).json.pickBranch and
        (__ \ 'field2).json.pickBranch
      ) reduce

      val js0 = Json.obj("field1" -> "alpha")
      val js = js0 ++ Json.obj("field2" -> Json.obj("field21" -> 123, "field22" -> true))    
      val js2 = js ++ Json.obj("field3" -> "beta")  
      js.validate(myReads) must beEqualTo(JsSuccess(js))
      js2.validate(myReads) must beEqualTo(JsSuccess(js))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "validate.error.missing-path"))
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
      js.validate(myReads) must beEqualTo(JsSuccess(Json.arr( "alpha", 123L, false)))
      js2.validate(myReads) must beEqualTo(JsSuccess(Json.arr( "alpha", 123L, false)))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "validate.error.missing-path") ++ JsError(__ \ 'field3, "validate.error.missing-path"))
    }

    "reduce Reads[JsArray] no type" in {
      import Reads._

      val myReads: Reads[JsArray] = (
        (__ \ 'field1).json.pick and
        (__ \ 'field2).json.pick and
        (__ \ 'field3).json.pick
      ) reduce

      val js0 = Json.obj("field1" -> "alpha")
      val js = js0 ++ Json.obj("field2" -> 123L, "field3" -> false)    
      val js2 = js ++ Json.obj("field4" -> false)  
      js.validate(myReads) must beEqualTo(JsSuccess(Json.arr( "alpha", 123L, false)))
      js2.validate(myReads) must beEqualTo(JsSuccess(Json.arr( "alpha", 123L, false)))
      js0.validate(myReads) must beEqualTo(JsError(__ \ 'field2, "validate.error.missing-path") ++ JsError(__ \ 'field3, "validate.error.missing-path"))
    }

    "serialize JsError to flat json" in {
      val jserr = JsError( Seq(
        (__ \ 'field1 \ 'field11) -> Seq(
          ValidationError("msg1.msg11", "arg11", 123L, 123.456F), 
          ValidationError("msg2.msg21.msg22", 456, 123.456, true, 123)
        ),
        (__ \ 'field2 \ 'field21) -> Seq(
          ValidationError("msg1.msg21", "arg1", Json.obj("test" -> "test2")),
          ValidationError("msg2", "arg1", "arg2") 
        )
      ))

      val flatJson = Json.obj(
        "obj.field1.field11" -> Json.arr(
          Json.obj( 
            "msg" -> "msg1.msg11", 
            "args" -> Json.arr("arg11",123,123.456F) 
          ),
          Json.obj(
            "msg" ->"msg2.msg21.msg22",
            "args" -> Json.arr(456,123.456,true,123)
          )
        ),
        "obj.field2.field21" -> Json.arr(
          Json.obj(
            "msg" -> "msg1.msg21",
            "args" -> Json.arr("arg1", Json.obj("test" -> "test2"))
          ),
          Json.obj(
            "msg" -> "msg2",
            "args" -> Json.arr("arg1","arg2")
          )
        )
      )

      JsError.toFlatJson(jserr) should beEqualTo(flatJson)
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
      ) reduce

      js.validate(myReads) must beEqualTo(JsSuccess(res))
    }
  }

  "JSON Writes" should {
    "manage option" in {
      import Writes._

      case class User(email: String, phone: Option[String])

      implicit val UserWrites = (
        (__ \ 'email).write[String] and
        (__ \ 'phone).writeOpt[String]
      )(unlift(User.unapply))

      Json.toJson(User("john.doe@blibli.com", None)) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com"))
      Json.toJson(User("john.doe@blibli.com", Some("12345678"))) must beEqualTo(Json.obj("email" -> "john.doe@blibli.com", "phone" -> "12345678"))
    }
  }


  "JSON Format" should {
    "manage option" in {
      import Reads._
      import Writes._

      case class User(email: String, phone: Option[String])

      implicit val UserFormat = (
        (__ \ 'email).format(email) and
        (__ \ 'phone).formatOpt(Format(minLength[String](8), Writes.of[String]))
      )(User, unlift(User.unapply))

      Json.obj("email" -> "john").validate[User] must beEqualTo(JsError(__ \ "email", ValidationError("validate.error.email")))
      Json.obj("email" -> "john.doe@blibli.com", "phone" -> "4").validate[User] must beEqualTo(JsError(__ \ "phone", ValidationError("validate.error.minlength", 8)))
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
      x must equalTo (JsSuccess(42))
    }

    "be a functor" in {
      "JsSuccess" in {
        val res1: JsResult[String] = JsSuccess("foo", JsPath(List(KeyPathNode("bar"))))
        res1.map(identity) must equalTo (res1)
      }

      "JsError" in {
        val res2: JsResult[String] = JsError(Seq(JsPath(List(KeyPathNode("bar"))) -> Seq(ValidationError("baz.bah"))))
        res2.map(identity) must equalTo (res2)
      }
    }
  }
}
