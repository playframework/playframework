package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._

import scala.util.control.Exception._
import java.text.ParseException

object JsonSpec extends Specification {

  case class User(id: Long, name: String, friends: List[User])

  implicit object UserFormat extends Format[User] {
    def reads(json: JsValue): User = User(
      (json \ "id").as[Long],
      (json \ "name").as[String],
      (json \ "friends").asOpt[List[User]].getOrElse(List()))
    def writes(u: User): JsValue = JsObject(List(
      "id" -> JsNumber(u.id),
      "name" -> JsString(u.name),
      "friends" -> JsArray(u.friends.map(fr => JsObject(List("id" -> JsNumber(fr.id), "name" -> JsString(fr.name)))))))
  }

  case class Car(id: Long, models: Map[String, String])

  implicit object CarFormat extends Format[Car] {
    def reads(json: JsValue): Car = Car(
      (json \ "id").as[Long], (json \ "models").as[Map[String, String]])
    def writes(c: Car): JsValue = JsObject(List(
      "id" -> JsNumber(c.id),
      "models" -> JsObject(c.models.map(x => x._1 -> JsString(x._2)).toList)))
  }

  import java.util.Date
  case class Post(body: String, created_at: Option[Date])

  import java.text.SimpleDateFormat
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'" // Iso8601 format (forgot timezone stuff)
  val dateParser = new SimpleDateFormat(dateFormat)

  // Try parsing date from iso8601 format
  implicit object DateFormat extends Reads[Date] {
    def reads(json: JsValue): Date = json match {
        // Need to throw a RuntimeException, ParseException beeing out of scope of asOpt
        case JsString(s) => catching(classOf[ParseException]).opt(dateParser.parse(s)).getOrElse(throw new RuntimeException("Parse exception"))
        case _ => throw new RuntimeException("Parse exception")
    }
  }

  implicit object PostFormat extends Format[Post] {
    def reads(json: JsValue): Post = Post(
      (json \ "body").as[String],
      (json \ "created_at").asOpt[Date])
    def writes(p: Post): JsValue = JsObject(List(
      "body" -> JsString(p.body))) // Don't care about creating created_at or not here
  }


  "JSON" should {
    "serialize and desarialize maps properly" in {
      val c = Car(1, Map("ford" -> "1954 model"))
      val jsonCar = toJson(c)
      jsonCar.as[Car] must equalTo(c)
    }
    "serialize and deserialize" in {
      val luigi = User(1, "Luigi", List())
      val kinopio = User(2, "Kinopio", List())
      val yoshi = User(3, "Yoshi", List())
      val mario = User(0, "Mario", List(luigi, kinopio, yoshi))
      val jsonMario = toJson(mario)
      jsonMario.as[User] must equalTo(mario)
      (jsonMario \\ "name") must equalTo(Seq(JsString("Mario"), JsString("Luigi"), JsString("Kinopio"), JsString("Yoshi")))
    }
    "Complete JSON should create full Post object" in {
      val postJson = """{"body": "foobar", "created_at": "2011-04-22T13:33:48Z"}"""
      val expectedPost = Post("foobar", Some(dateParser.parse("2011-04-22T13:33:48Z")))
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }
    "Optional parameters in JSON should generate post w/o date" in {
      val postJson = """{"body": "foobar"}"""
      val expectedPost = Post("foobar", None)
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }
    "Invalid parameters shoud be ignored" in {
      val postJson = """{"body": "foobar", "created_at":null}"""
      val expectedPost = Post("foobar", None)
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }
    "Can parse recursive object" in {
      val recursiveJson = """{"foo": {"foo":["bar"]}, "bar": {"foo":["bar"]}}"""
      val expectedJson = JsObject(List(
        "foo" -> JsObject(List(
          "foo" -> JsArray(List[JsValue](JsString("bar")))
          )),
        "bar" -> JsObject(List(
          "foo" -> JsArray(List[JsValue](JsString("bar")))
          ))
        ))
      val resultJson = Json.parse(recursiveJson)
      resultJson must equalTo(expectedJson)

    }
    "Can parse null values in Object" in {
      val postJson = """{"foo": null}"""
      val parsedJson = Json.parse(postJson)
      val expectedJson = JsObject(List("foo" -> JsNull))
      parsedJson must equalTo(expectedJson)
    }
    "Can parse null values in Array" in {
      val postJson = """[null]"""
      val parsedJson = Json.parse(postJson)
      val expectedJson = JsArray(List(JsNull))
      parsedJson must equalTo(expectedJson)
    }

    "Can parse multiple fields to Tuple2[String, String]" in {
      val postJson = """{ "foo1":"bar1", "foo2": "bar2" }"""  
      val parsedJson = Json.parse(postJson)
      val (foo1, foo2) = parsedJson.as[String, String]("foo1", "foo2")
      (foo1, foo2) must equalTo(("bar1", "bar2"))
    }

    "Can parse multiple JsPath to Tuple2[String, String]" in {
      val postJson = """{ "foo1": { "foo11" : "bar11", "foo12": "bar12" }, "foo2": "bar2" }"""  
      val parsedJson = Json.parse(postJson)
      val (foo1, foo2) = parsedJson.as[String, String](ROOT\"foo1"\"foo12", ROOT\"foo2")
      (foo1, foo2) must equalTo(("bar12", "bar2"))
    }

    "Parse Tuple2 from JsObject" >> {
      "With at least 2 fields (String, Boolean)" in {
        val postJson = """{ "foo1":"bar1", "foo2": true }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(String, Boolean)]
        (foo1, foo2) must equalTo(("bar1", true))
      }
      "With at least 2 fields (Short, Int)" in {
        val postJson = """{ "foo1":12, "foo2": 12345 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(Short, Int)]
        (foo1, foo2) must equalTo((12, 12345))
      }
      "With at least 2 fields (Long, Float)" in {
        val postJson = """{ "foo1":123456789, "foo2": 12345.789 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(Long, Float)]
        (foo1, foo2) must equalTo((123456789, 12345.789F))
      }
      "With at least 2 fields (Double, Array)" in {
        val postJson = """{ "foo1":12345678.12345678, "foo2": [ "bar1", "bar2", "bar3"] }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(Double, Array[String])]
        (foo1, foo2.toSeq) must equalTo((12345678.12345678, Seq("bar1", "bar2", "bar3")))
      }
      "With at least 2 fields (List, Map)" in {
        val postJson = """{ "foo1": [ "bar1", "bar2", "bar3"], "foo2": { "foo21": "bar21", "foo22": "bar22"} }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(List[String], Map[String, String])]
        (foo1, foo2) must equalTo((List("bar1", "bar2", "bar3"), Map( "foo21" -> "bar21", "foo22" -> "bar22")))
      }
      "With less than 2 fields, throws RuntimeException" in {
        val postJson = """{ "foo1":"bar1" }"""
        val parsedJson = Json.parse(postJson)
        parsedJson.as[(String, Int)] must throwA[RuntimeException](message = "Map with size>=2 expected")
      }
      "With more than 2 fields (String, String, String)" in {
        val postJson = """{ "foo1":"bar1", "foo2": "bar2", "foo3": "bar3" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2) = parsedJson.as[(String, String)]
        (foo1, foo2) must equalTo(("bar1", "bar2"))
      }
    }

    "Parse Tuple3 from JsObject" >> {
      "With at least 3 fields (String, Boolean, Int)" in {
        val postJson = """{ "foo1":"bar1", "foo2": true, "foo3": 123456 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(String, Boolean, Int)]
        (foo1, foo2, foo3) must equalTo(("bar1", true, 123456))
      }
      "With at least 3 fields (Short, Int, Float)" in {
        val postJson = """{ "foo1":12, "foo2": 12345, "foo3": 12345.789 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(Short, Int, Float)]
        (foo1, foo2, foo3) must equalTo((12, 12345, 12345.789F))
      }
      "With at least 3 fields (Long, Float, String)" in {
        val postJson = """{ "foo1":123456789, "foo2": 12345.789, "foo3": "bar3" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(Long, Float, String)]
        (foo1, foo2, foo3) must equalTo((123456789, 12345.789F, "bar3"))
      }
      "With at least 3 fields (Double, Array, List[Int])" in {
        val postJson = """{ "foo1":12345678.12345678, "foo2": [ "bar1", "bar2", "bar3"], "foo3": [ 123, 456, 789 ] }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(Double, Array[String], List[Int])]
        (foo1, foo2.toSeq, foo3) must equalTo((12345678.12345678, Seq("bar1", "bar2", "bar3"), List(123, 456, 789)))
      }
      "With at least 3 fields (List, Map, Map[String, Int])" in {
        val postJson = """{ "foo1": [ "bar1", "bar2", "bar3"], "foo2": { "foo21": "bar21", "foo22": "bar22"}, "foo3": { "foo21": 123, "foo22": 456} }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(List[String], Map[String, String], Map[String, Int])]
        (foo1, foo2, foo3) must equalTo((List("bar1", "bar2", "bar3"), Map( "foo21" -> "bar21", "foo22" -> "bar22"), Map( "foo21" -> 123, "foo22" -> 456)))
      }
      "With less than 3 fields, throws RuntimeException" in {
        val postJson = """{ "foo1":"bar1" }"""
        val parsedJson = Json.parse(postJson)
        parsedJson.as[(String, String, String)] must throwA[RuntimeException](message = "Map with size>=3 expected")
      }
      "With more than 3 fields (String, String, String, String)" in {
        val postJson = """{ "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3) = parsedJson.as[(String, String, String)]
        (foo1, foo2, foo3) must equalTo(("bar1", "bar2", "bar3"))
      }
    }
    "Parse Tuple4 from JsObject" >> {
      "With at least 4 fields (String, Boolean, Int, Float)" in {
        val postJson = """{ "foo1":"bar1", "foo2": true, "foo3": 123456, "foo4": 12345.789 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4) = parsedJson.as[(String, Boolean, Int, Float)]
        (foo1, foo2, foo3, foo4) must equalTo(("bar1", true, 123456, 12345.789F))
      }
      "With at least 4 fields (Short, Int, Float, Double)" in {
        val postJson = """{ "foo1":12, "foo2": 12345, "foo3": 12345.789, "foo4": 12345678.12345678 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4) = parsedJson.as[(Short, Int, Float, Double)]
        (foo1, foo2, foo3, foo4) must equalTo((12, 12345, 12345.789F, 12345678.12345678))
      }
      "With at least 4 fields (Array[String], List[String], List[Int], Map[String, String])" in {
        val postJson = """{ "foo1":[ "bar1", "bar2", "bar3"], "foo2": [ "bar1", "bar2", "bar3"], "foo3": [ 123, 456, 789 ], "foo4": { "foo41": "bar41", "foo42": "bar42"} }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4) = parsedJson.as[(Array[String], List[String], List[Int], Map[String, String])]
        (foo1.toSeq, foo2, foo3, foo4) must equalTo((Seq("bar1", "bar2", "bar3"), List("bar1", "bar2", "bar3"), List(123,456,789), Map( "foo41" -> "bar41", "foo42" -> "bar42")))
      }
      "With less than 4 fields, throws RuntimeException" in {
        val postJson = """{ "foo1":"bar1" }"""
        val parsedJson = Json.parse(postJson)
        parsedJson.as[(String, String, String, String)] must throwA[RuntimeException](message = "Map with size>=4 expected")
      }
      "With more than 4 fields (String, String, String, String, String)" in {
        val postJson = """{ "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4) = parsedJson.as[(String, String, String, String)]
        (foo1, foo2, foo3, foo4) must equalTo(("bar1", "bar2", "bar3", "bar4"))
      }
    }

    "Parse Tuple5 from JsObject" >> {
      "With at least 5 fields (String, Boolean, Short, Int, Float)" in {
        val postJson = """{ "foo1":"bar1", "foo2": true, "foo3": 123, "foo4": 123456, "foo5": 12345.678 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5) = parsedJson.as[(String, Boolean, Short, Int, Float)]
        (foo1, foo2, foo3, foo4, foo5) must equalTo(("bar1", true, 123, 123456, 12345.678F))
      }
      "With at least 5 fields (Double, Array[String], List[String], List[Int], Map[String, String])" in {
        val postJson = """{ "foo1": 12345678.12345678, "foo2":[ "bar1", "bar2", "bar3"], "foo3": [ "bar1", "bar2", "bar3"], "foo4": [ 123, 456, 789 ], "foo5": { "foo1": "bar1", "foo2": "bar2"} }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5) = parsedJson.as[(Double, Array[String], List[String], List[Int], Map[String, String])]
        (foo1, foo2.toSeq, foo3, foo4, foo5) must equalTo((12345678.12345678, Seq("bar1", "bar2", "bar3"), List("bar1", "bar2", "bar3"), List(123,456,789), Map( "foo1" -> "bar1", "foo2" -> "bar2")))
      }
      "With less than 5 fields, throws RuntimeException" in {
        val postJson = """{ "foo1":"bar1" }"""
        val parsedJson = Json.parse(postJson)
        parsedJson.as[(String, String, String, String, String)] must throwA[RuntimeException](message = "Map with size>=5 expected")
      }
      "With more than 5 fields (String, String, String, String, String, String)" in {
        val postJson = """{ "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", "foo6": "bar6" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5) = parsedJson.as[(String, String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5) must equalTo(("bar1", "bar2", "bar3", "bar4", "bar5"))
      }
    }

    "Parse Tuple6 from JsObject" >> {
      "With at least 6 fields (String, Boolean, Short, Int, Float, Double)" in {
        val postJson = """{ "foo1":"bar1", "foo2": true, "foo3": 123, "foo4": 123456, "foo5": 12345.678, "foo6": 12345678.12345678 }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, foo6) = parsedJson.as[(String, Boolean, Short, Int, Float, Double)]
        (foo1, foo2, foo3, foo4, foo5, foo6) must equalTo(("bar1", true, 123, 123456, 12345.678F, 12345678.12345678))
      }
      "With at least 6 fields (Array[String], Array[Int], List[String], List[Int], Map[String, String], Map[String, Long])" in {
        val postJson = """{ "foo1":[ "bar1", "bar2", "bar3"], "foo2": [ 123, 456, 789 ], "foo3": [ "bar1", "bar2", "bar3"], "foo4": [ 123, 456, 789 ], "foo5": { "foo1": "bar1", "foo2": "bar2"}, "foo6": { "foo1": 12345678, "foo2": 987654321} }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, foo6) = parsedJson.as[(Array[String], Array[Int], List[String], List[Int], Map[String, String], Map[String, Long])]
        (foo1.toSeq, foo2.toSeq, foo3, foo4, foo5, foo6) must equalTo((Seq("bar1", "bar2", "bar3"), Seq(123, 456, 789), List("bar1", "bar2", "bar3"), List(123, 456, 789), Map( "foo1" -> "bar1", "foo2" -> "bar2"), Map( "foo1" -> 12345678, "foo2" -> 987654321)))
      }
      "With less than 6 fields, throws RuntimeException" in {
        val postJson = """{ "foo1":"bar1" }"""
        val parsedJson = Json.parse(postJson)
        parsedJson.as[(String, String, String, String, String, String)] must throwA[RuntimeException](message = "Map with size>=6 expected")
      }
      "With more than 6 fields (String, String, String, String, String, String, String)" in {
        val postJson = """{ "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", "foo6": "bar6", "foo7": "bar7" }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, foo6) = parsedJson.as[(String, String, String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, foo6) must equalTo(("bar1", "bar2", "bar3", "bar4", "bar5", "bar6"))
      }
    }

    "Parse Tuple7 from JsObject" >> {
      "With at least 7 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7) = parsedJson.as[(String, String, String, String, String, 
                                                String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7"))
      }
    }

    "Parse Tuple8 from JsObject" >> {
      "With at least 8 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8"))
      }
    }

    "Parse Tuple9 from JsObject" >> {
      "With at least 9 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9"))
      }
    }

    "Parse Tuple10 from JsObject" >> {
      "With at least 10 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10"))
      }
    }

    "Parse Tuple11 from JsObject" >> {
      "With at least 11 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11"))
      }
    }

    "Parse Tuple12 from JsObject" >> {
      "With at least 12 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12"))
      }
    }

    "Parse Tuple13 from JsObject" >> {
      "With at least 13 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13"))
      }
    }

    "Parse Tuple14 from JsObject" >> {
      "With at least 14 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14"))
      }
    }

    "Parse Tuple15 from JsObject" >> {
      "With at least 15 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15"))
      }
    }

    "Parse Tuple16 from JsObject" >> {
      "With at least 16 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16"))
      }
    }

    "Parse Tuple17 from JsObject" >> {
      "With at least 17 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17"))
      }
    }

    "Parse Tuple18 from JsObject" >> {
      "With at least 18 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17", "foo18": "bar18"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17", "bar18"))
      }
    }

    "Parse Tuple19 from JsObject" >> {
      "With at least 19 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17", "foo18": "bar18", "foo19": "bar19"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17", "bar18", "bar19"))
      }
    }


    "Parse Tuple20 from JsObject" >> {
      "With at least 20 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17", "foo18": "bar18", "foo19": "bar19", "foo20": "bar20"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String, String, String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17", "bar18", "bar19", "bar20"))
      }
    }

    "Parse Tuple21 from JsObject" >> {
      "With at least 21 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17", "foo18": "bar18", "foo19": "bar19", "foo20": "bar20", 
          "foo21":"bar21"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20, 
          foo21) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20, 
          foo21) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17", "bar18", "bar19", "bar20", 
                        "bar21"))
      }
    }

    "Parse Tuple22 from JsObject" >> {
      "With at least 22 fields (String, ...)" in {
        val postJson = """{ 
          "foo1":"bar1", "foo2": "bar2", "foo3": "bar3", "foo4": "bar4", "foo5": "bar5", 
          "foo6":"bar6", "foo7": "bar7", "foo8": "bar8", "foo9": "bar9", "foo10": "bar10", 
          "foo11":"bar11", "foo12": "bar12", "foo13": "bar13", "foo14": "bar14", "foo15": "bar15", 
          "foo16":"bar16", "foo17": "bar17", "foo18": "bar18", "foo19": "bar19", "foo20": "bar20", 
          "foo21":"bar21", "foo22": "bar22"
        }"""
        val parsedJson = Json.parse(postJson)
        val (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20, 
          foo21, foo22) = parsedJson.as[(String, String, String, String, String, 
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String, String, String, String,
                                                String, String)]
        (foo1, foo2, foo3, foo4, foo5, 
          foo6, foo7, foo8, foo9, foo10, 
          foo11, foo12, foo13, foo14, foo15, 
          foo16, foo17, foo18, foo19, foo20, 
          foo21, foo22) must equalTo((
                        "bar1", "bar2", "bar3", "bar4", "bar5", 
                        "bar6", "bar7", "bar8", "bar9", "bar10", 
                        "bar11", "bar12", "bar13", "bar14", "bar15", 
                        "bar16", "bar17", "bar18", "bar19", "bar20", 
                        "bar21", "bar22"))
      }
    }
  }

}
