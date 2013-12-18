import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class FormSpec extends Specification {

  import play.api.data.mapping._
  import controllers.Application.helloValidation

  "HelloWorld form" should {

    "require all fields" in {

      val body = Map.empty[String, Seq[String]]
      val form = Form(body, helloValidation.validate(body))

      form.hasErrors must beTrue
      form.errors.size must equalTo(2)

      form("name").hasErrors must beTrue
      form("repeat").hasErrors must beTrue
      form("color").hasErrors must beFalse

      form.value must beNone
    }

    "require name" in {

      val body = Map("repeat" -> Seq("10"), "color" -> Seq("red"))
      val form = Form(body, helloValidation.validate(body))

      form.hasErrors must beTrue
      form.errors.size must equalTo(1)

      form("name").hasErrors must beTrue
      form("repeat").hasErrors must beFalse
      form("color").hasErrors must beFalse

      form.data.mapValues(_.head) must havePair("color" -> "red")
      form.data.mapValues(_.head) must havePair("repeat" -> "10")

      form("repeat").value must beSome.which(_ == "10")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beNone

      form.value must beNone
    }

    "validate repeat as numeric" in {

      val body = Map("name" -> Seq("Bob"), "repeat" -> Seq("xx"), "color" -> Seq("red"))
      val form = Form(body, helloValidation.validate(body))

      form.hasErrors must beTrue
      form.errors.size must equalTo(1)

      form("name").hasErrors must beFalse
      form("repeat").hasErrors must beTrue
      form("color").hasErrors must beFalse

      form.data.mapValues(_.head) must havePair("color" -> "red")
      form.data.mapValues(_.head) must havePair("repeat" -> "xx")
      form.data.mapValues(_.head) must havePair("name" -> "Bob")

      form("repeat").value must beSome.which(_ == "xx")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beSome.which(_ == "Bob")

      form.value must beNone
    }

    "be filled" in {

      val body = Map("name" -> Seq("Bob"), "repeat" -> Seq("10"), "color" -> Seq("red"))
      val form = Form(body, helloValidation.validate(body))

      form.hasErrors must beFalse

      form.data.mapValues(_.head) must havePair("color" -> "red")
      form.data.mapValues(_.head) must havePair("repeat" -> "10")
      form.data.mapValues(_.head) must havePair("name" -> "Bob")

      form("repeat").value must beSome.which(_ == "10")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beSome.which(_ == "Bob")

      form.value must beSome.which { _ match {
        case ("Bob", 10, Some("red")) => true
        case _ => false
      }}
    }

  }

}