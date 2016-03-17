/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data

import java.util
import java.util.Optional

import org.specs2.mutable.Specification
import play.mvc.Http.{ Context, Request, RequestBuilder }
import scala.collection.JavaConverters._
import scala.beans.BeanProperty
import play.api.{ Configuration, Environment }
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import play.data.FormFactory
import play.data.format.Formatters
import play.twirl.api.Html
import javax.validation.Validation

object FormSpec extends Specification {

  val messagesApi = new DefaultMessagesApi(Environment.simple(), Configuration.reference, new DefaultLangs(Configuration.reference))
  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
  val formFactory = new FormFactory(jMessagesApi, new Formatters(jMessagesApi), Validation.buildDefaultValidatorFactory().getValidator())

  "a java form" should {
    "be valid" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "done" -> Array("true"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to badly formatted date" in new WithApplication() {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().size() must beEqualTo(2)
      myForm.errors.get("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date")
      myForm.errors.get("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid")
      myForm.errors.get("dueDate").get(0).message() must beEqualTo("error.invalid.java.util.Date")
    }
    "have an error due to badly formatted date after using setTransientLang" in new WithApplication(GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq("en", "en-US", "fr")).build()) {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      Context.current.get().setTransientLang("fr");

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().size() must beEqualTo(3)
      myForm.errors.get("dueDate").get(0).messages().get(2) must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
      myForm.errors.get("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date") // is defined in play's default messages file
      myForm.errors.get("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid") // is defined in play's default messages file
      myForm.errors.get("dueDate").get(0).message() must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
    }
    "have an error due to badly formatted date after using changeLang" in new WithApplication(GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq("en", "en-US", "fr")).build()) {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      Context.current.get().changeLang("fr");

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().size() must beEqualTo(3)
      myForm.errors.get("dueDate").get(0).messages().get(2) must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
      myForm.errors.get("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date") // is defined in play's default messages file
      myForm.errors.get("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid") // is defined in play's default messages file
      myForm.errors.get("dueDate").get(0).message() must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
    }
    "have an error due to missing required value" in new WithApplication() {
      val req = dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().asScala must contain("error.required")
    }
    "have an error due to bad value in Id field" in new WithApplication() {
      val req = dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("id").get(0).messages().asScala must contain("error.invalid")
    }
    "be valid with default date binder" in {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to badly formatted date for default date binder" in new WithApplication() {
      val req = dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11e-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("endDate").get(0).messages().asScala must contain("error.invalid.java.util.Date")
    }

    "support repeated values for Java binding" in {

      val user1 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getName must beEqualTo("Kiki")
      user1.getEmails.size must beEqualTo(0)

      val user2 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com")))).get
      user2.getName must beEqualTo("Kiki")
      user2.getEmails.size must beEqualTo(1)

      val user3 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com"), "emails[1]" -> Array("kiki@zen.com")))).get
      user3.getName must beEqualTo("Kiki")
      user3.getEmails.size must beEqualTo(2)

      val user4 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com")))).get
      user4.getName must beEqualTo("Kiki")
      user4.getEmails.size must beEqualTo(1)

      val user5 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com", "kiki@zen.com")))).get
      user5.getName must beEqualTo("Kiki")
      user5.getEmails.size must beEqualTo(2)

    }

    "support optional deserialization of a common map" in {
      val data = new util.HashMap[String, String]()
      data.put("name", "kiwi")

      val userForm1: Form[AnotherUser] = formFactory.form(classOf[AnotherUser])
      val user1 = userForm1.bind(new java.util.HashMap[String, String]()).get()
      user1.getCompany.isPresent must beFalse

      data.put("company", "Acme")

      val userForm2: Form[AnotherUser] = formFactory.form(classOf[AnotherUser])
      val user2 = userForm2.bind(data).get()
      user2.getCompany.isPresent must beTrue
    }

    "support optional deserialization of a request" in {
      val user1 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getCompany.isPresent must beEqualTo(false)

      val user2 = formFactory.form(classOf[AnotherUser]).bindFromRequest(dummyRequest(Map("name" -> Array("Kiki"), "company" -> Array("Acme")))).get
      user2.getCompany.get must beEqualTo("Acme")
    }

    "bind when valid" in {
      val userForm: Form[MyUser] = formFactory.form(classOf[MyUser])
      val user = userForm.bind(new java.util.HashMap[String, String]()).get()
      userForm.hasErrors() must equalTo(false)
      (user == null) must equalTo(false)
    }

    "support email validation" in {
      val userEmail = formFactory.form(classOf[UserEmail])
      userEmail.bind(Map("email" -> "john@example.com").asJava).errors().asScala must beEmpty
      userEmail.bind(Map("email" -> "o'flynn@example.com").asJava).errors().asScala must beEmpty
      userEmail.bind(Map("email" -> "john@ex'ample.com").asJava).errors().asScala must not(beEmpty)
    }

    "support custom validators" in {
      "that fails when validator's condition is not met" in {
        val form = formFactory.form(classOf[Red])
        val bound = form.bind(Map("name" -> "blue").asJava)
        bound.hasErrors must_== true
        bound.hasGlobalErrors must_== true
        bound.globalErrors().asScala must not(beEmpty)
      }

      "that returns customized message when validator fails" in {
        val form = formFactory.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Shrek", "skinColor" -> "green", "hairColor" -> "blue").asJava)
        form.hasErrors must beEqualTo(true)
        form.errors().get("hairColor") must beNull
        val validationErrors = form.errors().get("skinColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("notblue")
      }

      "that returns customized message in annotation when validator fails" in {
        val form = formFactory.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Smurf", "skinColor" -> "blue", "hairColor" -> "white").asJava)
        form.errors().get("skinColor") must beNull
        form.hasErrors must beEqualTo(true)
        val validationErrors = form.errors().get("hairColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("i-am-blue")
      }

    }

    "work with the @repeat helper" in {
      val form = formFactory.form(classOf[JavaForm])

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
          Map.empty.asJava, Optional.empty[JavaForm], null, null, Validation.buildDefaultValidatorFactory().getValidator())
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
