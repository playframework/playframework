package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Generic._

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

  implicit val CarFormat:Format[Car] = productFormat2("id", "models")(Car)(Car.unapply)

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

    "Map[String,String] should be turned into JsValue" in {
      val f = toJson(Map("k"->"v"))
      f.toString must equalTo("{\"k\":\"v\"}")
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

    "Can parse null values in Array" in {
      val postJson = """[null]"""
      val parsedJson = Json.parse(postJson)
      val expectedJson = JsArray(List(JsNull))
      parsedJson must equalTo(expectedJson)
    }

    "Can read/write caseclasses with buildFormat1" in {
      case class Foo(bar1:String)
      implicit val FooFormat = buildFormat1("bar1")(Foo)(Foo.unapply)

      val postJson = """{"bar1":"chboing"}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      foo must equalTo(Foo("chboing"))
      toJson(foo).toString must equalTo(postJson)
    }

    "Can read/write caseclasses with buildFormat2" in {
      case class Foo(bar1:String, bar2:Int)
      implicit val FooFormat = buildFormat2("bar1", "bar2")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing","bar2":12345.0}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      foo must equalTo(Foo("chboing",12345))
      toJson(foo).toString must equalTo(postJson)
    }    

    "Can read/write caseclasses with buildFormat3" in {
      case class Foo(bar1:String, bar2:Int, bar3:Double)
      implicit val FooFormat = buildFormat3("bar1", "bar2", "bar3")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing","bar2":12345.0,"bar3":1.234578912345679E7}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      foo must equalTo(Foo("chboing",12345, 1.234578912345679E7))
      toJson(foo).toString must equalTo(postJson)
    }    

    "Can read/write caseclasses with buildFormat4" in {
      case class Foo(bar1:String, bar2:Int, bar3:Double, bar4:List[String])
      implicit val FooFormat = buildFormat4("bar1", "bar2", "bar3", "bar4")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing","bar2":12345.0,"bar3":1.234578912345679E7,"bar4":["bar41","bar42","bar43"]}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      foo must equalTo(Foo("chboing",12345,1.234578912345679E7,List("bar41","bar42","bar43")))
      toJson(foo).toString must equalTo(postJson)
    }    

    "Can read/write caseclasses with buildFormat5" in {
      case class Foo(bar1:String, bar2:Int, bar3:Double, bar4:List[String], bar5: Map[String, String])
      implicit val FooFormat = buildFormat5("bar1", "bar2", "bar3", "bar4", "bar5")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing","bar2":12345.0,"bar3":1.234578912345679E7,"bar4":["bar41","bar42","bar43"],"bar5":{"foo51":"bar51","foo52":"bar52","foo53":"bar53"}}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      foo must equalTo(Foo("chboing",12345,1.234578912345679E7,List("bar41","bar42","bar43"), Map( "foo51"->"bar51", "foo52"->"bar52", "foo53"->"bar53")))
      toJson(foo).toString must equalTo(postJson)
    } 

    "Can read/write caseclasses with buildFormat6" in {
      case class Foo(bar1:String, bar2:Int, bar3:Double, bar4:List[String], bar5: Map[String, String], bar6: Array[Int])
      implicit val FooFormat = buildFormat6("bar1", "bar2", "bar3", "bar4", "bar5", "bar6")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing","bar2":12345.0,"bar3":1.234578912345679E7,"bar4":["bar41","bar42","bar43"],"bar5":{"foo51":"bar51","foo52":"bar52","foo53":"bar53"},"bar6":[123.0,456.0,789.0]}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      // pb with equal between arrays
      val Foo(bar1, bar2, bar3, bar4, bar5, bar6) = foo
      (bar1, bar2, bar3, bar4, bar5, bar6.toSeq) must equalTo(("chboing",12345,1.234578912345679E7,List("bar41","bar42","bar43"), Map( "foo51"->"bar51", "foo52"->"bar52", "foo53"->"bar53"), Array(123,456,789).toSeq))
      toJson(foo).toString must equalTo(postJson)
    } 

    "Can read/write caseclasses with buildFormat10" in {
      case class Foo( bar1:String, bar2:String, bar3:String, bar4:String, bar5: String, 
                      bar6:String, bar7:String, bar8:String, bar9:String, bar10: String)
      implicit val FooFormat = buildFormat10("bar1", "bar2", "bar3", "bar4", "bar5", 
                                            "bar6", "bar7", "bar8", "bar9", "bar10")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing1","bar2":"chboing2","bar3":"chboing3","bar4":"chboing4","bar5":"chboing5","bar6":"chboing6","bar7":"chboing7","bar8":"chboing8","bar9":"chboing9","bar10":"chboing10"}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      // pb with equal between arrays
      foo must equalTo(Foo("chboing1", "chboing2", "chboing3", "chboing4", "chboing5", "chboing6", "chboing7", "chboing8", "chboing9", "chboing10"))
      toJson(foo).toString must equalTo(postJson)
    } 
  

    "Can read/write caseclasses with buildFormat22" in {
      case class Foo( bar1:String, bar2:String, bar3:String, bar4:String, bar5: String, 
                      bar6:String, bar7:String, bar8:String, bar9:String, bar10: String, 
                      bar11:String, bar12:String, bar13:String, bar14:String, bar15: String, 
                      bar16:String, bar17:String, bar18:String, bar19:String, bar20: String,
                      bar21:String, bar22:String)
      implicit val FooFormat = buildFormat22("bar1", "bar2", "bar3", "bar4", "bar5", 
                                            "bar6", "bar7", "bar8", "bar9", "bar10",
                                            "bar11", "bar12", "bar13", "bar14", "bar15",
                                            "bar16", "bar17", "bar18", "bar19", "bar20",
                                            "bar21", "bar22")(Foo)(Foo.unapply)

      // bar2 is a float just for the test as serialization of JsNumber gives a float
      val postJson = """{"bar1":"chboing1","bar2":"chboing2","bar3":"chboing3","bar4":"chboing4","bar5":"chboing5","bar6":"chboing6","bar7":"chboing7","bar8":"chboing8","bar9":"chboing9","bar10":"chboing10","bar11":"chboing11","bar12":"chboing12","bar13":"chboing13","bar14":"chboing14","bar15":"chboing15","bar16":"chboing16","bar17":"chboing17","bar18":"chboing18","bar19":"chboing19","bar20":"chboing20","bar21":"chboing21","bar22":"chboing22"}"""
      val parsedJson = Json.parse(postJson)
      val foo = parsedJson.as[Foo]
      // pb with equal between arrays
      foo must equalTo(Foo("chboing1", "chboing2", "chboing3", "chboing4", "chboing5", "chboing6", "chboing7", "chboing8", "chboing9", "chboing10", "chboing11", "chboing12", "chboing13", "chboing14", "chboing15", "chboing16", "chboing17", "chboing18", "chboing19", "chboing20", "chboing21", "chboing22"))
      toJson(foo).toString must equalTo(postJson)
    } 
  }
}
