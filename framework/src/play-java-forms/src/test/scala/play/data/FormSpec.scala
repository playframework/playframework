/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data

import java.util
import java.util.Optional
import javax.validation.{ Validation, Validator, Configuration => vConfiguration }
import javax.validation.groups.Default

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.specs2.mutable.Specification
import play.api.http.{ DefaultFileMimeTypesProvider, HttpConfiguration }
import play.api.i18n._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import play.api.{ Configuration, Environment }
import play.core.j.{ JavaContextComponents, JavaHelpers }
import play.data.format.Formatters
import play.mvc.Http.{ Context, Request, RequestBuilder }
import play.twirl.api.Html

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

class FormSpec extends Specification {

  val environment = Environment.simple()
  val config = Configuration.load(environment)
  val httpConfiguration = HttpConfiguration.fromConfiguration(config, environment)

  val langs = new DefaultLangsProvider(config).get
  val messagesApi = new DefaultMessagesApiProvider(environment, config, langs, httpConfiguration).get
  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)

  val defaultFileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
  val defaultContextComponents = JavaHelpers.createContextComponents(messagesApi, langs, defaultFileMimeTypes, httpConfiguration)
  val formFactory = new FormFactory(jMessagesApi, new Formatters(jMessagesApi), FormSpec.validator())

  "a java form" should {
    "be valid" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "done" -> Array("true"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to badly formatted date" in new WithApplication() {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().size() must beEqualTo(2)
      myForm.errors.get("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date")
      myForm.errors.get("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid")
      myForm.errors.get("dueDate").get(0).message() must beEqualTo("error.invalid.java.util.Date")

      // make sure we can access the values of an invalid form
      myForm.value().get().getId() must beEqualTo(1234567891)
      myForm.value().get().getName() must beEqualTo("peter")
    }
    "throws an exception when trying to access value of invalid form via get()" in new WithApplication() {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm.get must throwAn[IllegalStateException]
    }
    "allow to access the value of an invalid form even when not even one valid value was supplied" in new WithApplication() {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]
      val req = FormSpec.dummyRequest(Map("id" -> Array("notAnInt"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm.value().get().getId() must beEqualTo(0)
      myForm.value().get().getName() must_== null
    }
    "have an error due to badly formatted date after using setTransientLang" in new WithApplication(GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq("en", "en-US", "fr")).build()) {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]

      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

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
      val contextComponents = app.injector.instanceOf[JavaContextComponents]

      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

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
      val contextComponents = app.injector.instanceOf[JavaContextComponents]

      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("dueDate").get(0).messages().asScala must contain("error.required")
    }
    "have an error due to bad value in Id field" in new WithApplication() {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]

      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("id").get(0).messages().asScala must contain("error.invalid")
    }
    "be valid with default date binder" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "have an error due to badly formatted date for default date binder" in new WithApplication() {
      val contextComponents = app.injector.instanceOf[JavaContextComponents]

      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11e-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

      val myForm = formFactory.form(classOf[play.data.models.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors.get("endDate").get(0).messages().asScala must contain("error.invalid.java.util.Date")
    }

    "support repeated values for Java binding" in {

      val user1 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getName must beEqualTo("Kiki")
      user1.getEmails.size must beEqualTo(0)

      val user2 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com")))).get
      user2.getName must beEqualTo("Kiki")
      user2.getEmails.size must beEqualTo(1)

      val user3 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com"), "emails[1]" -> Array("kiki@zen.com")))).get
      user3.getName must beEqualTo("Kiki")
      user3.getEmails.size must beEqualTo(2)

      val user4 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com")))).get
      user4.getName must beEqualTo("Kiki")
      user4.getEmails.size must beEqualTo(1)

      val user5 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com", "kiki@zen.com")))).get
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
      val user1 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki")))).get
      user1.getCompany.isPresent must beEqualTo(false)

      val user2 = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "company" -> Array("Acme")))).get
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

      import play.core.j.PlayFormsMagicForJava._

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
          Map.empty.asJava, Optional.empty[JavaForm], null, null, FormSpec.validator())
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

    "return the appropriate constraints for the desired validation group(s)" in {
      "when NOT supplying a group all constraints that have the javax.validation.groups.Default group should be returned" in {
        // (When a constraint annotation doesn't define a "groups" attribute, it's default group will be Default.class by default)
        val myForm = formFactory.form(classOf[SomeUser])
        myForm.field("email").constraints().size() must beEqualTo(2)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("firstName").constraints().size() must beEqualTo(2)
        myForm.field("firstName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("firstName").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("lastName").constraints().size() must beEqualTo(3)
        myForm.field("lastName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("lastName").constraints().get(1)._1 must beEqualTo("constraint.minLength")
        myForm.field("lastName").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
        myForm.field("password").constraints().size() must beEqualTo(2)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("password").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(2)
        myForm.field("repeatPassword").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("repeatPassword").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
      }

      "when NOT supplying the Default.class group all constraints that have the javax.validation.groups.Default group should be returned" in {
        // The exact same tests again, but now we explicitly supply the Default.class group
        val myForm = formFactory.form(classOf[SomeUser], classOf[Default])
        myForm.field("email").constraints().size() must beEqualTo(2)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("firstName").constraints().size() must beEqualTo(2)
        myForm.field("firstName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("firstName").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("lastName").constraints().size() must beEqualTo(3)
        myForm.field("lastName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("lastName").constraints().get(1)._1 must beEqualTo("constraint.minLength")
        myForm.field("lastName").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
        myForm.field("password").constraints().size() must beEqualTo(2)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("password").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(2)
        myForm.field("repeatPassword").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("repeatPassword").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
      }

      "only return constraints for a specific group" in {
        // Only return the constraints for the PasswordCheck
        val myForm = formFactory.form(classOf[SomeUser], classOf[PasswordCheck])
        myForm.field("email").constraints().size() must beEqualTo(0)
        myForm.field("firstName").constraints().size() must beEqualTo(0)
        myForm.field("lastName").constraints().size() must beEqualTo(0)
        myForm.field("password").constraints().size() must beEqualTo(1)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(1)
        myForm.field("repeatPassword").constraints().get(0)._1 must beEqualTo("constraint.required")
      }

      "only return constraints for another specific group" in {
        // Only return the constraints for the LoginCheck
        val myForm = formFactory.form(classOf[SomeUser], classOf[LoginCheck])
        myForm.field("email").constraints().size() must beEqualTo(2)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.email")
        myForm.field("firstName").constraints().size() must beEqualTo(0)
        myForm.field("lastName").constraints().size() must beEqualTo(0)
        myForm.field("password").constraints().size() must beEqualTo(1)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(0)
      }

      "return constraints for two given groups" in {
        // Only return the required constraint for the LoginCheck and the PasswordCheck
        val myForm = formFactory.form(classOf[SomeUser], classOf[LoginCheck], classOf[PasswordCheck])
        myForm.field("email").constraints().size() must beEqualTo(2)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.email")
        myForm.field("firstName").constraints().size() must beEqualTo(0)
        myForm.field("lastName").constraints().size() must beEqualTo(0)
        myForm.field("password").constraints().size() must beEqualTo(1)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(1)
        myForm.field("repeatPassword").constraints().get(0)._1 must beEqualTo("constraint.required")
      }

      "return constraints for three given groups where on of them is the Default group" in {
        // Only return the required constraint for the LoginCheck, PasswordCheck and the Default group
        val myForm = formFactory.form(classOf[SomeUser], classOf[LoginCheck], classOf[PasswordCheck], classOf[Default])
        myForm.field("email").constraints().size() must beEqualTo(3)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.email")
        myForm.field("email").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
        myForm.field("firstName").constraints().size() must beEqualTo(2)
        myForm.field("firstName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("firstName").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("lastName").constraints().size() must beEqualTo(3)
        myForm.field("lastName").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("lastName").constraints().get(1)._1 must beEqualTo("constraint.minLength")
        myForm.field("lastName").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
        myForm.field("password").constraints().size() must beEqualTo(3)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("password").constraints().get(1)._1 must beEqualTo("constraint.minLength")
        myForm.field("password").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
        myForm.field("repeatPassword").constraints().size() must beEqualTo(3)
        myForm.field("repeatPassword").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("repeatPassword").constraints().get(1)._1 must beEqualTo("constraint.minLength")
        myForm.field("repeatPassword").constraints().get(2)._1 must beEqualTo("constraint.maxLength")
      }
    }

    "keep the declared order of constraint annotations" in {
      "return the constraints in the same order we declared them" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("email").constraints().size() must beEqualTo(8)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.min")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.max")
        myForm.field("email").constraints().get(2)._1 must beEqualTo("constraint.pattern")
        myForm.field("email").constraints().get(3)._1 must beEqualTo("constraint.validatewith")
        myForm.field("email").constraints().get(4)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(5)._1 must beEqualTo("constraint.minLength")
        myForm.field("email").constraints().get(6)._1 must beEqualTo("constraint.email")
        myForm.field("email").constraints().get(7)._1 must beEqualTo("constraint.maxLength")
      }

      "return the constraints in the same order we declared them, mixed with a non constraint annotation" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("name").constraints().size() must beEqualTo(8)
        myForm.field("name").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("name").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("name").constraints().get(2)._1 must beEqualTo("constraint.email")
        myForm.field("name").constraints().get(3)._1 must beEqualTo("constraint.max")
        myForm.field("name").constraints().get(4)._1 must beEqualTo("constraint.minLength")
        myForm.field("name").constraints().get(5)._1 must beEqualTo("constraint.pattern")
        myForm.field("name").constraints().get(6)._1 must beEqualTo("constraint.validatewith")
        myForm.field("name").constraints().get(7)._1 must beEqualTo("constraint.min")
      }

      "return the constraints of a superclass in the same order we declared them" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("password").constraints().size() must beEqualTo(8)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("password").constraints().get(1)._1 must beEqualTo("constraint.validatewith")
        myForm.field("password").constraints().get(2)._1 must beEqualTo("constraint.max")
        myForm.field("password").constraints().get(3)._1 must beEqualTo("constraint.required")
        myForm.field("password").constraints().get(4)._1 must beEqualTo("constraint.min")
        myForm.field("password").constraints().get(5)._1 must beEqualTo("constraint.maxLength")
        myForm.field("password").constraints().get(6)._1 must beEqualTo("constraint.pattern")
        myForm.field("password").constraints().get(7)._1 must beEqualTo("constraint.email")
      }
    }
  }

}

object FormSpec {

  def dummyRequest(data: Map[String, Array[String]]): Request = {
    new RequestBuilder()
      .uri("http://localhost/test")
      .bodyFormArrayValues(data.asJava)
      .build()
  }

  def validator(): Validator = {
    val validationConfig: vConfiguration[_] = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator())
    validationConfig.buildValidatorFactory().getValidator()
  }

}

class JavaForm(@BeanProperty var foo: java.util.List[JavaSubForm]) {
  def this() = this(null)
}
class JavaSubForm(@BeanProperty var a: String, @BeanProperty var b: String) {
  def this() = this(null, null)
}
