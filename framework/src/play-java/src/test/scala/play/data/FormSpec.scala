/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data

import org.specs2.mutable.Specification
import play.mvc._
import play.mvc.Http.{ Context, Request, RequestBuilder }
import scala.collection.JavaConverters._
import scala.beans.BeanProperty
import play.libs.F
import play.twirl.api.Html

object FormSpec extends Specification {

  "a java form" should {
    "be valid" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "done" -> Array("true"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = Form.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = Form.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to baldy formatted date" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = Form.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().asScala must contain("error.invalid.java.util.Date")
    }
    "have an error due to missing required value" in {
      val req = dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = Form.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().asScala must contain("error.required")
    }
    "have an error due to bad value in Id field" in {
      val req = dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = Form.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("id").get(0).messages().asScala must contain("error.invalid")
    }

    "support repeated values for Java binding" in {

      val user1 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getName must beEqualTo("Kiki")
      user1.getEmails.size must beEqualTo(0)

      val user2 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com")))).get
      user2.getName must beEqualTo("Kiki")
      user2.getEmails.size must beEqualTo(1)

      val user3 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com"), "emails[1]" -> Array("kiki@zen.com")))).get
      user3.getName must beEqualTo("Kiki")
      user3.getEmails.size must beEqualTo(2)

      val user4 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com")))).get
      user4.getName must beEqualTo("Kiki")
      user4.getEmails.size must beEqualTo(1)

      val user5 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com", "kiki@zen.com")))).get
      user5.getName must beEqualTo("Kiki")
      user5.getEmails.size must beEqualTo(2)

    }

    "support option deserialization" in {
      val user1 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getCompany.isDefined must beEqualTo(false)

      val user2 = Form.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "company" -> Array("Acme")))).get
      user2.getCompany.get must beEqualTo("Acme")
    }

    "bind when valid" in {
      val userForm: Form[MyUser] = Form.form(classOf[MyUser])
      val user = userForm.bind(new java.util.HashMap[String, String]()).get()
      userForm.hasErrors() must equalTo(false)
      (user == null) must equalTo(false)
    }

    "support email validation" in {
      val userEmail = Form.form(classOf[UserEmail])
      userEmail.bind(Map("email" -> "john@example.com").asJava).errors().asScala must beEmpty
      userEmail.bind(Map("email" -> "o'flynn@example.com").asJava).errors().asScala must beEmpty
      userEmail.bind(Map("email" -> "john@ex'ample.com").asJava).errors().asScala must not beEmpty
    }

    "support custom validators" in {
      "that fails when validator's condition is not met" in {
        val form = Form.form(classOf[Red])
        val bound = form.bind(Map("name" -> "blue").asJava)
        bound.hasErrors must_== true
        bound.hasGlobalErrors must_== true
        bound.globalErrors().asScala must not beEmpty
      }

      "that returns customized message when validator fails" in {
        val form = Form.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Shrek", "skinColor" -> "green", "hairColor" -> "blue").asJava)
        form.hasErrors must beEqualTo(true)
        form.errors().get("hairColor") must beNull
        val validationErrors = form.errors().get("skinColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("notblue")
      }

      "that returns customized message in annotation when validator fails" in {
        val form = Form.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Smurf", "skinColor" -> "blue", "hairColor" -> "white").asJava)
        form.errors().get("skinColor") must beNull
        form.hasErrors must beEqualTo(true)
        val validationErrors = form.errors().get("hairColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("i-am-blue")
      }

    }

    "work with the @repeat helper" in {
      val form = Form.form(classOf[JavaForm])

      import play.core.j.PlayMagicForJava._

      def render(form: Form[_], min: Int = 1) = views.html.helper.repeat.apply(form("foo"), min) { f =>
        val a = f("a")
        val b = f("b")
        Html(s"${a.name}=${a.value.getOrElse("")},${b.name}=${b.value.getOrElse("")}")
      }.map(_.toString)

      def fillNoBind(values: (String, String)*) = {
        val map = values.zipWithIndex.flatMap {
          case ((a, b), i) => Seq("foo[" + i + "].a" -> a, "foo[" + i + "].b" -> b)
        }.toMap
        // Don't use bind, the point here is to have a form with data that isn't bound, otherwise the mapping indexes
        // used come from the form, not the input data
        new Form[JavaForm](null, classOf[JavaForm], map.asJava,
          Map.empty.asJava, F.None().asInstanceOf[F.Option[JavaForm]], null)
      }

      "render the right number of fields if there's multiple sub fields at a given index when filled from a value" in {
        render(
          form.fill(new JavaForm(List(new JavaSubForm("somea", "someb")).asJava))
        ) must exactly("foo[0].a=somea,foo[0].b=someb")
      }

      "render the right number of fields if there's multiple sub fields at a given index when filled from a form" in {
        render(
          fillNoBind("somea" -> "someb")
        ) must exactly("foo[0].a=somea,foo[0].b=someb")
      }

      "get the order of the fields correct when filled from a value" in {
        render(
          form.fill(new JavaForm(List(new JavaSubForm("a", "b"), new JavaSubForm("c", "d"),
            new JavaSubForm("e", "f"), new JavaSubForm("g", "h")).asJava))
        ) must exactly("foo[0].a=a,foo[0].b=b", "foo[1].a=c,foo[1].b=d",
            "foo[2].a=e,foo[2].b=f", "foo[3].a=g,foo[3].b=h").inOrder
      }

      "get the order of the fields correct when filled from a form" in {
        render(
          fillNoBind("a" -> "b", "c" -> "d", "e" -> "f", "g" -> "h")
        ) must exactly("foo[0].a=a,foo[0].b=b", "foo[1].a=c,foo[1].b=d",
            "foo[2].a=e,foo[2].b=f", "foo[3].a=g,foo[3].b=h").inOrder
      }
    }
  }

  def dummyRequest(data: Map[String, Array[String]]): Request = {
    new RequestBuilder()
      .uri("http://localhost/test")
      .bodyFormArrayValues(data.asJava)
      .build()
  }

}

class JavaForm(@BeanProperty var foo: java.util.List[JavaSubForm]) {
  def this() = this(null)
}
class JavaSubForm(@BeanProperty var a: String, @BeanProperty var b: String) {
  def this() = this(null, null)
}
