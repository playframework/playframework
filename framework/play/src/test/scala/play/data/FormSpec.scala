package play.data

import org.specs2.mutable._
import play.mvc._
import play.mvc.Http.Context
import scala.collection.JavaConverters._
class DummyRequest(data: Map[String, Array[String]]) extends play.mvc.Http.Request {
  def uri() = "/test"
  def method() = "GET"
  def path() = "test"
  def urlFormEncoded() = data.asJava
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
}

