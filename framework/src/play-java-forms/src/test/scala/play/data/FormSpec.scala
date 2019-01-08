/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data

import java.nio.file.{ Files, Path }
import java.util
import java.util.{ Collections, Optional }
import java.time.{ LocalDate, ZoneId }

import akka.stream.javadsl.{ FileIO, Source }
import akka.util.ByteString
import javax.validation.{ Validation, ValidatorFactory, Configuration => vConfiguration }
import javax.validation.groups.Default

import com.typesafe.config.{ Config, ConfigFactory }
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.specs2.mutable.Specification
import play.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import play.api.Application
import play.components.TemporaryFileComponents
import play.core.j.JavaContextComponents
import play.data.validation.ValidationError
import play.libs.F.Tuple
import play.libs.Files.{ TemporaryFile, TemporaryFileCreator }
import play.mvc.{ EssentialFilter, Http }
import play.mvc.Http.{ Context, Request, RequestBody, RequestBuilder }
import play.mvc.Http.MultipartFormData.{ DataPart, FilePart, Part }
import play.routing.Router
import play.twirl.api.Html

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

class RuntimeDependencyInjectionFormSpec extends FormSpec {
  private var app: Option[Application] = None

  override def defaultContextComponents: JavaContextComponents = app.getOrElse(application()).injector.instanceOf[JavaContextComponents]

  override def formFactory: FormFactory = app.getOrElse(application()).injector.instanceOf[FormFactory]

  override def tempFileCreator: TemporaryFileCreator = app.getOrElse(application()).injector.instanceOf[TemporaryFileCreator]

  override def application(extraConfig: (String, Any)*): Application = {
    val builtApp = GuiceApplicationBuilder().configure(extraConfig.toMap).build()
    app = Option(builtApp)
    builtApp
  }
}

class CompileTimeDependencyInjectionFormSpec extends FormSpec {

  class MyComponents(context: ApplicationLoader.Context, extraConfig: Map[String, Any] = Map.empty) extends BuiltInComponentsFromContext(context)
    with FormFactoryComponents with TemporaryFileComponents {
    override def router(): Router = Router.empty()

    override def httpFilters(): java.util.List[EssentialFilter] = java.util.Collections.emptyList()

    override def config(): Config = {
      val javaExtraConfig = extraConfig.mapValues {
        case v: Seq[Any] => v.asJava
        case v => v
      }.asJava
      ConfigFactory.parseMap(javaExtraConfig).withFallback(super.config())
    }
  }

  private var components: Option[MyComponents] = None
  private lazy val context = ApplicationLoader.create(play.Environment.simple())

  override def formFactory: FormFactory = components.getOrElse{
    new MyComponents(context)
  }.formFactory()

  override def tempFileCreator: TemporaryFileCreator = components.getOrElse{
    new MyComponents(context)
  }.tempFileCreator()

  override def application(extraConfig: (String, Any)*): Application = {
    val myComponents = new MyComponents(context, extraConfig.toMap)
    components = Option(myComponents)
    myComponents.application().asScala()
  }

  override def defaultContextComponents: JavaContextComponents = components.getOrElse(new MyComponents(ApplicationLoader.create(play.Environment.simple()))).javaContextComponents()
}

trait CommonFormSpec extends Specification {
  def checkFileParts(fileParts: Seq[FilePart[TemporaryFile]], key: String, contentType: String, filename: String, fileContent: String, dispositionType: String = "form-data") = {
    fileParts.foreach(filePart => {
      filePart.getKey must equalTo(key)
      filePart.getDispositionType must equalTo(dispositionType)
      filePart.getContentType must equalTo(contentType)
      filePart.getFilename must equalTo(filename)
      Files.readAllLines(filePart.getRef.path()) must equalTo(List(fileContent).asJava)
    })
  }

  def createTemporaryFile(suffix: String, content: String)(implicit temporaryFileCreator: TemporaryFileCreator): TemporaryFile = {
    val file = temporaryFileCreator.create("temp", suffix)
    Files.write(file.path(), content.getBytes())
    file
  }

  def createThesisTemporaryFiles()(implicit temporaryFileCreator: TemporaryFileCreator): Map[String, TemporaryFile] = Map(
    "thesisDocFile" -> createTemporaryFile("pdf", "by Lightbend founder Martin Odersky"),
    "latexFile" -> createTemporaryFile("tex", "the final draft"),
    "codesnippetsFile" -> createTemporaryFile("scala", "some code snippets"),
    "bibliographyBrianGoetz" -> createTemporaryFile("epub", "Java Concurrency in Practice"),
    "bibliographyJamesGosling" -> createTemporaryFile("mobi", "The Java Programming Language")
  )

  def createThesisRequestWithFileParts(files: Map[String, TemporaryFile]) = FormSpec.dummyMultipartRequest(
    Map("title" -> Array("How Scala works")),
    List(
      new FilePart[TemporaryFile]("document", "best_thesis.pdf", "application/pdf", files("thesisDocFile")),
      new FilePart[TemporaryFile]("attachments[]", "final_draft.tex", "application/x-tex", files("latexFile")),
      new FilePart[TemporaryFile]("attachments[]", "examples.scala", "text/x-scala-source", files("codesnippetsFile")),
      new FilePart[TemporaryFile]("bibliography[0]", "Java_Concurrency_in_Practice.epub", "application/epub+zip", files("bibliographyBrianGoetz")),
      new FilePart[TemporaryFile]("bibliography[1]", "The-Java-Programming-Language.mobi", "application/x-mobipocket-ebook", files("bibliographyJamesGosling"))
    ))
}

trait FormSpec extends CommonFormSpec {

  sequential

  def formFactory: FormFactory
  def tempFileCreator: TemporaryFileCreator
  def application(extraConfig: (String, Any)*): Application
  def defaultContextComponents: JavaContextComponents

  "a java form" should {

    "with a root name" should {
      "be valid with all fields" in {
        val req = FormSpec.dummyRequest(Map("task.id" -> Array("1234567891"), "task.name" -> Array("peter"), "task.dueDate" -> Array("15/12/2009"), "task.endDate" -> Array("2008-11-21")))
        Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

        val myForm = formFactory.form("task", classOf[play.data.Task]).bindFromRequest()
        myForm hasErrors () must beEqualTo(false)
      }
      "be valid with all fields with direct field access" in {
        val req = FormSpec.dummyRequest(Map("task.id" -> Array("1234567891"), "task.name" -> Array("peter"), "task.dueDate" -> Array("15/12/2009"), "task.endDate" -> Array("2008-11-21")))

        val myForm = formFactory.form("task", classOf[play.data.Subtask]).withDirectFieldAccess(true).bindFromRequest(req)
        myForm hasErrors () must beEqualTo(false)
      }
      "allow to access the value of an invalid form prefixing fields with the root name" in new WithApplication(application()) {
        val req = FormSpec.dummyRequest(Map("task.id" -> Array("notAnInt"), "task.name" -> Array("peter"), "task.done" -> Array("true"), "task.dueDate" -> Array("15/12/2009")))
        Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

        val myForm = formFactory.form("task", classOf[play.data.Task]).bindFromRequest()

        myForm hasErrors () must beEqualTo(true)
        myForm.field("task.name").value.asScala must beSome("peter")
      }
      "have an error due to missing required value" in new WithApplication(application()) {
        val contextComponents = defaultContextComponents

        val req = FormSpec.dummyRequest(Map("task.id" -> Array("1234567891x"), "task.name" -> Array("peter")))
        Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, contextComponents))

        val myForm = formFactory.form("task", classOf[play.data.Task]).bindFromRequest()
        myForm hasErrors () must beEqualTo(true)
        myForm.errors("task.dueDate").get(0).messages().asScala must contain("error.required")
      }
      "have an error due to missing required value with direct field access" in new WithApplication(application()) {
        val req = FormSpec.dummyRequest(Map("task.id" -> Array("1234567891x"), "task.name" -> Array("peter")))

        val myForm = formFactory.form("task", classOf[play.data.Subtask]).withDirectFieldAccess(true).bindFromRequest(req)
        myForm hasErrors () must beEqualTo(true)
        myForm.errors("task.dueDate").get(0).messages().asScala must contain("error.required")
      }
    }
    "be valid with all fields" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with all fields with direct field access" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11-21")))

      val myForm = formFactory.form(classOf[play.data.Subtask]).withDirectFieldAccess(true).bindFromRequest(req)
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with all fields with direct field access switched on in config" in new WithApplication(application("play.forms.binding.directFieldAccess" -> "true")) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11-21")))

      val myForm = formFactory.form(classOf[play.data.Subtask]).bindFromRequest(req)
      myForm hasErrors () must beEqualTo(false)
    }
    "be valid with mandatory params passed" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
    }
    "query params ignored when using POST" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "POST", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getName() must beEqualTo("peter")
      myForm.value().get().getId() must beEqualTo(null)
    }
    "query params ignored when using PUT" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "PUT", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getName() must beEqualTo("peter")
      myForm.value().get().getId() must beEqualTo(null)
    }
    "query params ignored when using PATCH" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "PATCH", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getName() must beEqualTo("peter")
      myForm.value().get().getId() must beEqualTo(null)
    }

    "query params NOT ignored when using GET" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "GET", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() must beEqualTo(LocalDate.of(2009, 12, 15)) // we also parse the body for GET requests
      myForm.value().get().getName() must beEqualTo("michael") // but query param overwrites body when using GET
      myForm.value().get().getId() must beEqualTo(55555)
    }
    "query params NOT ignored when using DELETE" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "DELETE", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() must beEqualTo(LocalDate.of(2009, 12, 15)) // we also parse the body for DELETE requests
      myForm.value().get().getName() must beEqualTo("michael") // but query param overwrites body when using DELETE
      myForm.value().get().getId() must beEqualTo(55555)
    }
    "query params NOT ignored when using HEAD" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "HEAD", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() must beEqualTo(LocalDate.of(2009, 12, 15)) // we also parse the body for HEAD requests
      myForm.value().get().getName() must beEqualTo("michael") // but query param overwrites body when using HEAD
      myForm.value().get().getId() must beEqualTo(55555)
    }
    "query params NOT ignored when using OPTIONS" in {
      val req = FormSpec.dummyRequest(Map("name" -> Array("peter"), "dueDate" -> Array("15/12/2009")), "OPTIONS", "?name=michael&id=55555")
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(false)
      myForm.value().get().getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() must beEqualTo(LocalDate.of(2009, 12, 15)) // we also parse the body for OPTIONS requests
      myForm.value().get().getName() must beEqualTo("michael") // but query param overwrites body when using OPTIONS
      myForm.value().get().getId() must beEqualTo(55555)
    }

    "have an error due to badly formatted date" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("dueDate").get(0).messages().size() must beEqualTo(2)
      myForm.errors("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date")
      myForm.errors("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid")
      myForm.errors("dueDate").get(0).message() must beEqualTo("error.invalid.java.util.Date")

      // make sure we can access the values of an invalid form
      myForm.value().get().getId() must beEqualTo(1234567891)
      myForm.value().get().getName() must beEqualTo("peter")
    }
    "throws an exception when trying to access value of invalid form via get()" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm.get must throwAn[IllegalStateException]
    }
    "allow to access the value of an invalid form even when not even one valid value was supplied" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("notAnInt"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm.value().get().getId() must_== null
      myForm.value().get().getName() must_== null
    }
    "have an error due to badly formatted date after using setTransientLang" in new WithApplication(application("play.i18n.langs" -> Seq("en", "en-US", "fr"))) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      Context.current.get().setTransientLang("fr")

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("dueDate").get(0).messages().size() must beEqualTo(3)
      myForm.errors("dueDate").get(0).messages().get(2) must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
      myForm.errors("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date") // is defined in play's default messages file
      myForm.errors("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid") // is defined in play's default messages file
      myForm.errors("dueDate").get(0).message() must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
    }
    "have an error due to badly formatted date after using changeLang" in new WithApplication(application("play.i18n.langs" -> Seq("en", "en-US", "fr"))) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("2009/11e/11")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      Context.current.get().changeLang("fr")

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("dueDate").get(0).messages().size() must beEqualTo(3)
      myForm.errors("dueDate").get(0).messages().get(2) must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
      myForm.errors("dueDate").get(0).messages().get(1) must beEqualTo("error.invalid.java.util.Date") // is defined in play's default messages file
      myForm.errors("dueDate").get(0).messages().get(0) must beEqualTo("error.invalid") // is defined in play's default messages file
      myForm.errors("dueDate").get(0).message() must beEqualTo("error.invalid.dueDate") // is ONLY defined in messages.fr
    }
    "have an error due to missing required value" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("dueDate").get(0).messages().asScala must contain("error.required")
    }
    "have an error due to missing required value with direct field access" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))

      val myForm = formFactory.form(classOf[play.data.Subtask]).withDirectFieldAccess(true).bindFromRequest(req)
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("dueDate").get(0).messages().asScala must contain("error.required")
    }
    "be invalid when only fields (and no getters) exist but direct field access is disabled" in {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter")))

      formFactory.form(classOf[play.data.Subtask]).bindFromRequest(req) must throwA[IllegalStateException].like {
        case e: IllegalStateException =>
          e.getMessage must beMatching("""JSR-303 validated property '.*' does not have a corresponding accessor for data binding - check your DataBinder's configuration \(bean property versus direct field access\)""")
      }
    }
    "have an error due to bad value in Id field" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891x"), "name" -> Array("peter"), "dueDate" -> Array("12/12/2009")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("id").get(0).messages().asScala must contain("error.invalid")
    }

    "have an error due to badly formatted date for default date binder" in new WithApplication(application()) {
      val req = FormSpec.dummyRequest(Map("id" -> Array("1234567891"), "name" -> Array("peter"), "dueDate" -> Array("15/12/2009"), "endDate" -> Array("2008-11e-21")))
      Context.current.set(new Context(666, null, req, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava, defaultContextComponents))

      val myForm = formFactory.form(classOf[play.data.Task]).bindFromRequest()
      myForm hasErrors () must beEqualTo(true)
      myForm.errors("endDate").get(0).messages().asScala must contain("error.invalid.java.util.Date")
    }

    "support repeated values for Java binding" in {

      val user1form = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"))))
      val user1 = user1form.get
      user1form.field("name").indexes() must beEqualTo(List.empty.asJava)
      user1.getName must beEqualTo("Kiki")
      user1.getEmails.size must beEqualTo(0)

      val user2form = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com"))))
      val user2 = user2form.get
      user2form.field("emails").indexes() must beEqualTo(List(0).asJava)
      user2.getName must beEqualTo("Kiki")
      user2.getEmails.size must beEqualTo(1)

      val user3form = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[0]" -> Array("kiki@gmail.com"), "emails[1]" -> Array("kiki@zen.com"))))
      val user3 = user3form.get
      user3form.field("emails").indexes() must beEqualTo(List(0, 1).asJava)
      user3.getName must beEqualTo("Kiki")
      user3.getEmails.size must beEqualTo(2)

      val user4form = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com"))))
      val user4 = user4form.get
      user4form.field("emails").indexes() must beEqualTo(List(0).asJava)
      user4.getName must beEqualTo("Kiki")
      user4.getEmails.size must beEqualTo(1)

      val user5form = formFactory.form(classOf[AnotherUser]).bindFromRequest(FormSpec.dummyRequest(Map("name" -> Array("Kiki"), "emails[]" -> Array("kiki@gmail.com", "kiki@zen.com"))))
      val user5 = user5form.get
      user5form.field("emails").indexes() must beEqualTo(List(0, 1).asJava)
      user5.getName must beEqualTo("Kiki")
      user5.getEmails.size must beEqualTo(2)

    }

    "support optional deserialization of a common map" in {
      val data = new util.HashMap[String, String]()
      data.put("name", "Kiki")

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

    "bind files" should {

      "be valid with all fields" in new WithApplication(application()) {
        implicit val temporaryFileCreator = tempFileCreator

        val files = createThesisTemporaryFiles()

        try {
          val req = createThesisRequestWithFileParts(files)

          val myForm = formFactory.form(classOf[play.data.Thesis]).bindFromRequest(req)

          myForm.hasErrors() must beEqualTo(false)
          myForm.hasGlobalErrors() must beEqualTo(false)

          myForm.rawData().size() must beEqualTo(1)
          myForm.files().size() must beEqualTo(5)

          val thesis = myForm.get

          thesis.getTitle must beEqualTo("How Scala works")
          myForm.field("title").value().asScala must beSome("How Scala works")
          myForm.field("title").file().asScala must beNone

          checkFileParts(Seq(thesis.getDocument, myForm.field("document").file().get()), "document", "application/pdf", "best_thesis.pdf", "by Lightbend founder Martin Odersky")
          myForm.field("document").value().asScala must beNone

          thesis.getAttachments().size() must beEqualTo(2)
          myForm.field("attachments").indexes() must beEqualTo(List(0, 1).asJava)
          checkFileParts(Seq(thesis.getAttachments().get(0), myForm.field("attachments[0]").file().get()), "attachments[]", "application/x-tex", "final_draft.tex", "the final draft")
          myForm.field("attachments[0]").value().asScala must beNone
          checkFileParts(Seq(thesis.getAttachments().get(1), myForm.field("attachments[1]").file().get()), "attachments[]", "text/x-scala-source", "examples.scala", "some code snippets")
          myForm.field("attachments[1]").value().asScala must beNone

          thesis.getBibliography().size() must beEqualTo(2)
          myForm.field("bibliography").indexes() must beEqualTo(List(0, 1).asJava)
          checkFileParts(Seq(thesis.getBibliography().get(0), myForm.field("bibliography[0]").file().get()), "bibliography[0]", "application/epub+zip", "Java_Concurrency_in_Practice.epub", "Java Concurrency in Practice")
          myForm.field("bibliography[0]").value().asScala must beNone
          checkFileParts(Seq(thesis.getBibliography().get(1), myForm.field("bibliography[1]").file().get()), "bibliography[1]", "application/x-mobipocket-ebook", "The-Java-Programming-Language.mobi", "The Java Programming Language")
          myForm.field("bibliography[1]").value().asScala must beNone
        } finally {
          files.values.foreach(temporaryFileCreator.delete(_))
        }
      }

      "have an error due to missing required file" in new WithApplication(application()) {
        val myForm = formFactory.form(classOf[play.data.Thesis]).bindFromRequest(FormSpec.dummyMultipartRequest(Map("title" -> Array("How Scala works"))))
        myForm hasErrors () must beEqualTo(true)
        myForm hasGlobalErrors () must beEqualTo(false)
        myForm.errors().size() must beEqualTo(3)
        myForm.files().size() must beEqualTo(0)
        myForm.error("document").get.message() must beEqualTo("error.required")
        myForm.error("attachments").get.message() must beEqualTo("error.required")
        myForm.error("bibliography").get.message() must beEqualTo("error.required")
      }
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
          Map("name" -> "Shrek", "skinColor" -> "green", "hairColor" -> "blue", "nailColor" -> "darkblue").asJava)
        form.hasErrors must beEqualTo(true)
        form.errors("hairColor").asScala must beEmpty
        form.errors("nailColor").asScala must beEmpty
        val validationErrors = form.errors("skinColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("notblue")
        validationErrors.get(0).arguments().size must beEqualTo(2)
        validationErrors.get(0).arguments().get(0) must beEqualTo("argOne")
        validationErrors.get(0).arguments().get(1) must beEqualTo("argTwo")
      }

      "that returns customized message in annotation when validator fails" in {
        val form = formFactory.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Smurf", "skinColor" -> "blue", "hairColor" -> "white", "nailColor" -> "darkblue").asJava)
        form.errors("skinColor").asScala must beEmpty
        form.errors("nailColor").asScala must beEmpty
        form.hasErrors must beEqualTo(true)
        val validationErrors = form.errors("hairColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("i-am-blue")
        validationErrors.get(0).arguments().size must beEqualTo(0)
      }

      "that returns customized message when validator fails even when args param from getErrorMessageKey is null" in {
        val form = formFactory.form(classOf[MyBlueUser]).bind(
          Map("name" -> "Nemo", "skinColor" -> "blue", "hairColor" -> "blue", "nailColor" -> "yellow").asJava)
        form.errors("skinColor").asScala must beEmpty
        form.errors("hairColor").asScala must beEmpty
        form.hasErrors must beEqualTo(true)
        val validationErrors = form.errors("nailColor")
        validationErrors.size() must beEqualTo(1)
        validationErrors.get(0).message must beEqualTo("notdarkblue")
        validationErrors.get(0).arguments().size must beEqualTo(0)
      }

    }

    "support type arguments constraints" in {
      val listForm = formFactory.form(classOf[TypeArgumentForm]).bindFromRequest(FormSpec.dummyRequest(Map(
        "list[0]" -> Array("4"), "list[1]" -> Array("-3"), "list[2]" -> Array("6"),
        "map['ab']" -> Array("28"), "map['something']" -> Array("2"), "map['worksperfect']" -> Array("87"),
        "optional" -> Array("Acme")
      )))

      listForm.hasErrors must beEqualTo(true)
      listForm.errors().size() must beEqualTo(4)
      listForm.errors("list[1]").get(0).messages().size() must beEqualTo(1)
      listForm.errors("list[1]").get(0).messages().get(0) must beEqualTo("error.min")
      listForm.value().get().getList.get(0) must beEqualTo(4)
      listForm.value().get().getList.get(1) must beEqualTo(-3)
      listForm.value().get().getList.get(2) must beEqualTo(6)
      listForm.errors("map[ab]").get(0).messages().get(0) must beEqualTo("error.minLength")
      listForm.value().get().getMap.get("ab") must beEqualTo(28)
      listForm.errors("map[something]").get(0).messages().get(0) must beEqualTo("error.min")
      listForm.value().get().getMap.get("something") must beEqualTo(2)
      listForm.value().get().getMap.get("worksperfect") must beEqualTo(87)
      listForm.errors("optional").get(0).messages().get(0) must beEqualTo("error.minLength")
      listForm.value().get().getOptional.get must beEqualTo("Acme")
      // Also test an Optional that binds a value but doesn't cause a validation error:
      val optForm = formFactory.form(classOf[TypeArgumentForm]).bindFromRequest(FormSpec.dummyRequest(Map(
        "optional" -> Array("Microsoft Corporation")
      )))
      optForm.errors().size() must beEqualTo(0)
      optForm.get().getOptional.get must beEqualTo("Microsoft Corporation")
    }

    "support @repeatable constraints" in {
      val form = formFactory.form(classOf[RepeatableConstraintsForm]).bind(Map("name" -> "xyz").asJava)
      form.field("name").constraints().size() must beEqualTo(4)
      form.field("name").constraints().get(0)._1 must beEqualTo("constraint.validatewith")
      form.field("name").constraints().get(1)._1 must beEqualTo("constraint.validatewith")
      form.field("name").constraints().get(2)._1 must beEqualTo("constraint.pattern")
      form.field("name").constraints().get(3)._1 must beEqualTo("constraint.pattern")
      form.hasErrors must beEqualTo(true)
      form.hasGlobalErrors() must beEqualTo(false)
      form.errors().size() must beEqualTo(4)
      form.errors("name").size() must beEqualTo(4)
      val nameErrorMessages = form.errors("name").asScala.flatMap(_.messages().asScala)
      nameErrorMessages.size must beEqualTo(4)
      nameErrorMessages must contain("Should be a - c")
      nameErrorMessages must contain("Should be c - h")
      nameErrorMessages must contain("notgreen")
      nameErrorMessages must contain("notblue")
    }

    "work with the @repeat helper" in {
      val form = formFactory.form(classOf[JavaForm])

      import play.core.j.PlayFormsMagicForJava._

      def render(form: Form[_], min: Int = 1) = views.html.helper.repeat.apply(form("foo"), min) { f =>
        val a = f("a")
        val b = f("b")
        Html(s"${a.name}=${a.value.getOrElse("")},${b.name}=${b.value.getOrElse("")}")
      }.map(_.toString)

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

    "work with the @repeatWithIndex helper" in {
      val form = formFactory.form(classOf[JavaForm])

      import play.core.j.PlayFormsMagicForJava._

      def render(form: Form[_], min: Int = 1) = views.html.helper.repeatWithIndex.apply(form("foo"), min) { (f, i) =>
        val a = f("a")
        val b = f("b")
        Html(s"${a.name}=${a.value.getOrElse("")}${i},${b.name}=${b.value.getOrElse("")}${i}")
      }.map(_.toString)

      "render the right number of fields if there's multiple sub fields at a given index when filled from a value" in {
        render(
          form.fill(new JavaForm(List(new JavaSubForm("somea", "someb")).asJava))
        ) must exactly("foo[0].a=somea0,foo[0].b=someb0")
      }

      "render the right number of fields if there's multiple sub fields at a given index when filled from a form" in {
        render(
          fillNoBind("somea" -> "someb")
        ) must exactly("foo[0].a=somea0,foo[0].b=someb0")
      }

      "get the order of the fields correct when filled from a value" in {
        render(
          form.fill(new JavaForm(List(new JavaSubForm("a", "b"), new JavaSubForm("c", "d"),
            new JavaSubForm("e", "f"), new JavaSubForm("g", "h")).asJava))
        ) must exactly("foo[0].a=a0,foo[0].b=b0", "foo[1].a=c1,foo[1].b=d1",
            "foo[2].a=e2,foo[2].b=f2", "foo[3].a=g3,foo[3].b=h3").inOrder
      }

      "get the order of the fields correct when filled from a form" in {
        render(
          fillNoBind("a" -> "b", "c" -> "d", "e" -> "f", "g" -> "h")
        ) must exactly("foo[0].a=a0,foo[0].b=b0", "foo[1].a=c1,foo[1].b=d1",
            "foo[2].a=e2,foo[2].b=f2", "foo[3].a=g3,foo[3].b=h3").inOrder
      }
    }

    "correctly calculate indexes()" in {

      val dataPart = Map(
        "someDataField[0]" -> "foo",
        "someDataField[1]" -> "foo",
        "someDataField[2]" -> "foo",
        "someDataField[4]" -> "foo",
        "someDataField[59]" -> "foo"
      ).asJava

      val filePart = Map(
        "someFileField[0]" -> null,
        "someFileField[13]" -> null,
        "someFileField[24]" -> null,
        "someFileField[85]" -> null
      ).asJava.asInstanceOf[util.Map[String, Http.MultipartFormData.FilePart[_]]]

      val form: Form[Object] = new Form(null, null, dataPart, filePart, null, Optional.empty[Object](), null, null, null, null, null, null)

      val dataField = new Form.Field(form, "someDataField", null, null, null, null, null)
      dataField.indexes() must beEqualTo(List(0, 1, 2, 4, 59).asJava)

      val fileField = new Form.Field(form, "someFileField", null, null, null, null, null)
      fileField.indexes() must beEqualTo(List(0, 13, 24, 85).asJava)
    }

    def fillNoBind(values: (String, String)*) = {
      val map = values.zipWithIndex.flatMap {
        case ((a, b), i) => Seq("foo[" + i + "].a" -> a, "foo[" + i + "].b" -> b)
      }.toMap
      // Don't use bind, the point here is to have a form with data that isn't bound, otherwise the mapping indexes
      // used come from the form, not the input data
      new Form[JavaForm](null, classOf[JavaForm], map.asJava,
        List.empty.asJava.asInstanceOf[java.util.List[ValidationError]], Optional.empty[JavaForm], null, null, FormSpec.validatorFactory(), ConfigFactory.load())
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

    "respect the order of validation groups defined via group sequences" in {
      "first group gets validated and already fails and therefore second group wont even get validated anymore" in {
        val myForm = formFactory.form(classOf[SomeUser], classOf[OrderedChecks]).bind(Map("email" -> "invalid_email", "password" -> "", "repeatPassword" -> "").asJava)
        // first group
        myForm.errors("email").size() must beEqualTo(1)
        myForm.errors("email").get(0).message() must beEqualTo("error.email")
        myForm.errors("password").size() must beEqualTo(1)
        myForm.errors("password").get(0).message() must beEqualTo("error.required")
        // next group
        myForm.errors("repeatPassword").size() must beEqualTo(0)
      }
      "first group gets validated and already succeeds but then second group fails" in {
        val myForm = formFactory.form(classOf[SomeUser], classOf[OrderedChecks]).bind(Map("email" -> "larry@google.com", "password" -> "asdfasdf", "repeatPassword" -> "").asJava)
        // first group
        myForm.errors("email").size() must beEqualTo(0)
        myForm.errors("password").size() must beEqualTo(0)
        // next group
        myForm.errors("repeatPassword").size() must beEqualTo(1)
        myForm.errors("repeatPassword").get(0).message() must beEqualTo("error.required")
      }
      "all group gets validated and succeed" in {
        val myForm = formFactory.form(classOf[SomeUser], classOf[OrderedChecks]).bind(Map("email" -> "larry@google.com", "password" -> "asdfasdf", "repeatPassword" -> "asdfasdf").asJava)
        // first group
        myForm.errors("email").size() must beEqualTo(0)
        myForm.errors("password").size() must beEqualTo(0)
        // next group
        myForm.errors("repeatPassword").size() must beEqualTo(0)
        myForm.hasErrors() must beEqualTo(false)
        myForm.hasGlobalErrors() must beEqualTo(false)
      }
    }

    "honor its validate method" in {
      "when it returns an error object" in {
        val myForm = formFactory.form(classOf[SomeUser]).bind(Map("password" -> "asdfasdf", "repeatPassword" -> "vwxyz").asJava)
        myForm.error("password").get.message() must beEqualTo ("Passwords do not match")
      }
      "when it returns an null (error) object" in {
        val myForm = formFactory.form(classOf[SomeUser]).bind(Map("password" -> "asdfasdf", "repeatPassword" -> "asdfasdf").asJava)
        myForm.globalErrors().size() must beEqualTo(0)
        myForm.errors("password").size() must beEqualTo(0)
      }
      "when it returns an error object but is skipped because its not in validation group" in {
        val myForm = formFactory.form(classOf[SomeUser], classOf[LoginCheck]).bind(Map("password" -> "asdfasdf", "repeatPassword" -> "vwxyz").asJava)
        myForm.error("password").isPresent must beFalse
      }
      "when it returns a string" in {
        val myForm = formFactory.form(classOf[LoginUser]).bind(Map("email" -> "fail@google.com").asJava)
        myForm.globalErrors().size() must beEqualTo(1)
        myForm.globalErrors().get(0).message() must beEqualTo("Invalid email provided!")
      }
      "when it returns an empty string" in {
        val myForm = formFactory.form(classOf[LoginUser]).bind(Map("email" -> "bill.gates@microsoft.com").asJava)
        myForm.globalErrors().size() must beEqualTo(1)
        myForm.globalErrors().get(0).message() must beEqualTo("")
      }
      "when it returns an error list" in {
        val myForm = formFactory.form(classOf[AnotherUser]).bind(Map("name" -> "Bob Marley").asJava)
        myForm.globalErrors().size() must beEqualTo(1)
        myForm.globalErrors().get(0).message() must beEqualTo("Form could not be processed")
        myForm.errors("name").size() must beEqualTo(1)
        myForm.errors("name").get(0).message() must beEqualTo("Name not correct")
      }
      "when it returns an empty error list" in {
        val myForm = formFactory.form(classOf[AnotherUser]).bind(Map("name" -> "Kiki").asJava)
        myForm.globalErrors().size() must beEqualTo(0)
        myForm.errors().size() must beEqualTo(0)
        myForm.errors("name").size() must beEqualTo(0)
      }
    }

    "not process it's legacy validate method when the Validatable interface is implemented" in {
      val myForm = formFactory.form(classOf[LegacyUser]).bind(Map("foo" -> "foo").asJava)
      myForm.globalErrors().size() must beEqualTo(0)
    }

    "keep the declared order of constraint annotations" in {
      "return the constraints in the same order we declared them" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("email").constraints().size() must beEqualTo(6)
        myForm.field("email").constraints().get(0)._1 must beEqualTo("constraint.pattern")
        myForm.field("email").constraints().get(1)._1 must beEqualTo("constraint.validatewith")
        myForm.field("email").constraints().get(2)._1 must beEqualTo("constraint.required")
        myForm.field("email").constraints().get(3)._1 must beEqualTo("constraint.minLength")
        myForm.field("email").constraints().get(4)._1 must beEqualTo("constraint.email")
        myForm.field("email").constraints().get(5)._1 must beEqualTo("constraint.maxLength")
      }

      "return the constraints in the same order we declared them, mixed with a non constraint annotation" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("name").constraints().size() must beEqualTo(6)
        myForm.field("name").constraints().get(0)._1 must beEqualTo("constraint.required")
        myForm.field("name").constraints().get(1)._1 must beEqualTo("constraint.maxLength")
        myForm.field("name").constraints().get(2)._1 must beEqualTo("constraint.email")
        myForm.field("name").constraints().get(3)._1 must beEqualTo("constraint.minLength")
        myForm.field("name").constraints().get(4)._1 must beEqualTo("constraint.pattern")
        myForm.field("name").constraints().get(5)._1 must beEqualTo("constraint.validatewith")
      }

      "return the constraints of a superclass in the same order we declared them" in {
        val myForm = formFactory.form(classOf[LoginUser])
        myForm.field("password").constraints().size() must beEqualTo(6)
        myForm.field("password").constraints().get(0)._1 must beEqualTo("constraint.minLength")
        myForm.field("password").constraints().get(1)._1 must beEqualTo("constraint.validatewith")
        myForm.field("password").constraints().get(2)._1 must beEqualTo("constraint.required")
        myForm.field("password").constraints().get(3)._1 must beEqualTo("constraint.maxLength")
        myForm.field("password").constraints().get(4)._1 must beEqualTo("constraint.pattern")
        myForm.field("password").constraints().get(5)._1 must beEqualTo("constraint.email")
      }
    }
  }

}

object FormSpec {

  def dummyRequest(data: Map[String, Array[String]], method: String = "POST", query: String = ""): Request = {
    new RequestBuilder()
      .method(method)
      .uri("http://localhost/test" + query)
      .bodyFormArrayValues(data.asJava)
      .build()
  }

  def dummyMultipartRequest(dataParts: Map[String, Array[String]] = Map.empty, fileParts: List[FilePart[TemporaryFile]] = List.empty): Request = {
    new RequestBuilder().method("POST").uri("http://localhost/test").build().withBody(
      new RequestBody(new Http.MultipartFormData[TemporaryFile] {
        override def asFormUrlEncoded(): util.Map[String, Array[String]] = dataParts.asJava
        override def getFiles: util.List[FilePart[TemporaryFile]] = fileParts.asJava
      }))
  }

  def validatorFactory(): ValidatorFactory = {
    val validationConfig: vConfiguration[_] = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator())
    validationConfig.buildValidatorFactory()
  }

}

class JavaForm(@BeanProperty var foo: java.util.List[JavaSubForm]) {
  def this() = this(null)
}
class JavaSubForm(@BeanProperty var a: String, @BeanProperty var b: String) {
  def this() = this(null, null)
}
