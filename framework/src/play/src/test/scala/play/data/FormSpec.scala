package play.data

import org.specs2.mutable.Specification
import play.mvc._
import play.mvc.Http.Context
import scala.collection.JavaConverters._

class DummyRequest(data: Map[String, Array[String]]) extends play.mvc.Http.Request {
  def uri() = "/test"
  def method() = "GET"
  def path() = "test"
  def host() = "localhost"
  def acceptLanguages = new java.util.ArrayList[play.i18n.Lang]
  def accept = List("text/html").asJava
  def accepts(mediaType: String) = false
  def headers() = new java.util.HashMap[String, Array[String]]()
  val remoteAddress = "127.0.0.1"
  def body() = new Http.RequestBody {
    override def asFormUrlEncoded = data.asJava
    override def asRaw = null
    override def asText = null
    override def asXml = null
    override def asJson = null
  }
  def cookies() = new play.mvc.Http.Cookies {
    def get(name: String) = null
  }
  def queryString: java.util.Map[String, Array[String]] = new java.util.HashMap()
  setUsername("peter")
}

object ScalaForms {
   import play.api.data.validation.Constraints._
   import play.api.data._
   import play.api.data.Forms._
   import format.Formats._

   case class User(name: String, age: Int)

    val userForm = Form(
      mapping(
        "name" -> of[String].verifying(nonEmpty),
        "age" -> of[Int].verifying(min(0), max(100))
      )(User.apply)(User.unapply)
    )

    val loginForm = Form(
      tuple(
        "email" -> of[String],
        "password" -> of[Int])
      )

    val helloForm = Form(
      tuple(
        "name" -> nonEmptyText,
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
        "14" -> optional(text)
      )
    )

    val form = Form(
          "foo" -> Forms.text.verifying("first.digit", s => (s.headOption map {_ == '3'}) getOrElse false)
                     .transform[Int](Integer.parseInt _, _.toString).verifying("number.42", _ < 42)
      )
}
object FormSpec extends Specification {

  "A form" should {
    "be valid" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "done" -> Array("true"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to baldy formatted date" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11/11")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)

    }
    "have an error due to bad value in Id field" in {
      val req = new DummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(req, Map.empty.asJava, Map.empty.asJava))

      val myForm = Controller.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)

    }
    
    "apply constraints on wrapped mappings" in {
      
      
      
      "when it binds data" in {
        val f1 = ScalaForms.form.bind(Map("foo"->"0"))
        f1.errors.size must equalTo (1)
        f1.errors.find(_.message == "first.digit") must beSome

        val f2 = ScalaForms.form.bind(Map("foo"->"3"))
        f2.errors.size must equalTo (0)

        val f3 = ScalaForms.form.bind(Map("foo"->"50"))
        f3.errors.size must equalTo (1) // Only one error because "number.42" can’t be applied since wrapped bind failed
        f3.errors.find(_.message == "first.digit") must beSome

        val f4 = ScalaForms.form.bind(Map("foo"->"333"))
        f4.errors.size must equalTo (1)
        f4.errors.find(_.message == "number.42") must beSome
      }
      
      "when it is filled with data" in {
        val f1 = ScalaForms.form.fillAndValidate(0)
        f1.errors.size must equalTo (1)
        f1.errors.find(_.message == "first.digit") must beSome

        val f2 = ScalaForms.form.fillAndValidate(3)
        f2.errors.size must equalTo (0)

        val f3 = ScalaForms.form.fillAndValidate(50)
        f3.errors.size must equalTo (2)
        f3.errors.find(_.message == "first.digit") must beSome
        f3.errors.find(_.message == "number.42") must beSome

        val f4 = ScalaForms.form.fillAndValidate(333)
        f4.errors.size must equalTo (1)
        f4.errors.find(_.message == "number.42") must beSome
      }
    }
  }

  "render form using field[Type] syntax" in {
    
    val anyData = Map("email" -> "bob@gmail.com", "password" -> "123")
    ScalaForms.loginForm.bind(anyData).get.toString must equalTo("(bob@gmail.com,123)")
  }

  "render a form with max 18 fields" in {
    
    ScalaForms.helloForm.bind(Map("name" -> "foo", "repeat" -> "1")).get.toString must equalTo("(foo,1,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)")
  }

  "render a from using java" in {
    import play.data._
    val userForm: Form[MyUser] = play.mvc.Controller.form(classOf[MyUser])
    val user = userForm.bind(new java.util.HashMap[String, String]()).get()
    userForm.hasErrors() must equalTo(false)
    (user == null) must equalTo(false)
  }
}

