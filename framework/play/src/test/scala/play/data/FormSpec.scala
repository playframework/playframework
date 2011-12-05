package play.data

import org.specs2.mutable._
import play.mvc._
import play.mvc.Http.Context
import scala.collection.JavaConverters._

class DummyRequest(data: Map[String, Array[String]]) extends play.mvc.Http.Request {
  def uri() = "/test"
  def method() = "GET"
  def path() = "test"
  def body() = new Http.RequestBody {
    def asUrlFormEncoded = data.asJava
    def asRaw = null
    def asText = null
    def asXml = null
  }
  def queryString: java.util.Map[String, Array[String]] = new java.util.HashMap()
  setUsername("peter")
}

object FormSpec extends Specification {

  "A form" should {
    "be valid" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "done" -> Array("true"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to baldy formatted date" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11/11")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest
      myForm hasErrors () must beEqualTo(true)

    }
    "have an error due to bad value in Id field" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest
      myForm hasErrors () must beEqualTo(true)

    }
  }

  "render form using field[Type] syntax" in {
    import play.api.data.validation.Constraints._
    import play.api.data._
    import format.Formats._
    case class User(name: String, age: Int)

    val userForm = Form(
      of(User)(
        "name" -> of[String].verifying(required),
        "age" -> of[Int].verifying(min(0), max(100))))
    val loginForm = Form(
      of(
        "email" -> of[String],
        "password" -> of[Int]))
    val anyData = Map("email" -> "bob@gmail.com", "password" -> "123")
    loginForm.bind(anyData).get.toString must equalTo("(bob@gmail.com,123)")
  }

  "render a form with max 18 fields" in {
    import play.api.data._

    val helloForm = Form(
      of(
        "name" -> requiredText,
        "repeat" -> number(min = 1, max = 100),
        "color" -> optional(text),
        "still works" -> optional(text),
        "1" -> optional(text),
        "2" -> optional(text),
        "3" -> optional(text),
        "4" -> optional(text),
        "5" -> optional(text),
        "6" -> optional(text),
        "7" -> optional(text),
        "8" -> optional(text),
        "9" -> optional(text),
        "10" -> optional(text),
        "11" -> optional(text),
        "12" -> optional(text),
        "13" -> optional(text),
        "14" -> optional(text)))
    helloForm.bind(Map("name" -> "foo", "repeat" -> "1")).get.toString must equalTo("(foo,1,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)")
  }

  "render a from using java" in {
    import play.data._
    val userForm: Form[MyUser] = play.mvc.Controller.form(classOf[MyUser])
    val user = userForm.bind(new java.util.HashMap[String, String]()).get()
    userForm.hasErrors() must equalTo(false)
    (user == null) must equalTo(false)
  }
}

