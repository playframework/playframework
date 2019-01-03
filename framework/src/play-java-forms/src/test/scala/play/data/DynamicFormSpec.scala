/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data

import com.typesafe.config.ConfigFactory
import java.nio.file.Files

import javax.validation.Validation

import org.specs2.mutable.Specification
import play.api.i18n.DefaultMessagesApi
import play.core.j.PlayFormsMagicForJava.javaFieldtoScalaField
import play.data.format.Formatters
import play.libs.Files.{ SingletonTemporaryFileCreator, TemporaryFile }
import play.mvc.Http.MultipartFormData.FilePart
import views.html.helper.FieldConstructor.defaultField
import views.html.helper.inputText

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
 * Specs for Java dynamic forms
 */
class DynamicFormSpec extends CommonFormSpec {

  val messagesApi = new DefaultMessagesApi()
  implicit val messages = messagesApi.preferred(Seq.empty)
  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
  val validatorFactory = FormSpec.validatorFactory()
  val config = ConfigFactory.load()

  "a dynamic form" should {

    "bind values from a request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.get("foo") must_== "bar"
      form.value("foo").get must_== "bar"
    }

    "bind values from a multipart request containing files" in {
      implicit val temporaryFileCreator = new SingletonTemporaryFileCreator()

      val files = createThesisTemporaryFiles()

      try {
        val req = createThesisRequestWithFileParts(files)

        val myForm = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(req)

        myForm.hasErrors() must beEqualTo(false)
        myForm.hasGlobalErrors() must beEqualTo(false)

        myForm.rawData().size() must beEqualTo(1)
        myForm.files().size() must beEqualTo(5)

        myForm.get("title") must beEqualTo("How Scala works")
        myForm.field("title").value().asScala must beSome("How Scala works")
        myForm.field("title").file().asScala must beNone

        checkFileParts(Seq(myForm.file("document"), myForm.field("document").file().get()), "document", "application/pdf", "best_thesis.pdf", "by Lightbend founder Martin Odersky")
        myForm.field("document").value().asScala must beNone

        // TODO: This tests below are buggy. As you can see the closing bracket ] is missing everywhere. This is a bug in spring-beans.
        // When parsing e.g. "data[attachments[0]]" spring just looks for the first occurrence of ] when and uses it for the the end position in substring
        // See here, `keyEnd` is wrong:
        // https://github.com/spring-projects/spring-framework/blob/v5.1.3.RELEASE/spring-beans/src/main/java/org/springframework/beans/AbstractNestablePropertyAccessor.java#L941

        checkFileParts(Seq(myForm.file("attachments[0"), myForm.field("attachments[0").file().get()), "attachments[]", "application/x-tex", "final_draft.tex", "the final draft")
        myForm.field("attachments[0").value().asScala must beNone
        checkFileParts(Seq(myForm.file("attachments[1"), myForm.field("attachments[1").file().get()), "attachments[]", "text/x-scala-source", "examples.scala", "some code snippets")
        myForm.field("attachments[1").value().asScala must beNone

        checkFileParts(Seq(myForm.file("bibliography[0"), myForm.field("bibliography[0").file().get()), "bibliography[0]", "application/epub+zip", "Java_Concurrency_in_Practice.epub", "Java Concurrency in Practice")
        myForm.field("bibliography[0").value().asScala must beNone
        checkFileParts(Seq(myForm.file("bibliography[1"), myForm.field("bibliography[1").file().get()), "bibliography[1]", "application/x-mobipocket-ebook", "The-Java-Programming-Language.mobi", "The Java Programming Language")
        myForm.field("bibliography[1").value().asScala must beNone
      } finally {
        files.values.foreach(temporaryFileCreator.delete(_))
      }
    }

    "allow access to raw data values from request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.rawData().get("foo") must_== "bar"
    }

    "display submitted values in template helpers" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      val html = inputText(form("foo")).body
      html must contain("value=\"bar\"")
      html must contain("name=\"foo\"")
    }

    "render correctly when no value is submitted in template helpers" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map()))
      val html = inputText(form("foo")).body
      html must contain("value=\"\"")
      html must contain("name=\"foo\"")
    }

    "display errors in template helpers" in {
      var form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form = form.withError("foo", "There was an error")
      val html = inputText(form("foo")).body
      html must contain("There was an error")
    }

    "display errors when a field is not present" in {
      var form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).bindFromRequest(FormSpec.dummyRequest(Map()))
      form = form.withError("foo", "Foo is required")
      val html = inputText(form("foo")).body
      html must contain("Foo is required")
    }

    "allow access to the property when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form.get("foo") must_== "bar"
      form.value("foo").get must_== "bar"
    }

    "allow access to the equivalent of the raw data when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form("foo").value().get() must_== "bar"
    }

    "fail with exception when trying to switch on direct field access" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config)
      form.withDirectFieldAccess(true) must throwA[RuntimeException].like {
        case e => e.getMessage must endWith("Not possible to enable direct field access for dynamic forms.")
      }
    }

    "work when switch off direct field access" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validatorFactory, config).withDirectFieldAccess(false).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.get("foo") must_== "bar"
      form.value("foo").get must_== "bar"
    }

    "don't throw NullPointerException when all components of form are null" in {
      val form = new DynamicForm(null, null, null, null).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form("foo").value().get() must_== "bar"
    }

    "convert jField to scala Field when all components of jField are null" in {
      val jField = new play.data.Form.Field(null, null, null, null, null, null, null)
      jField.indexes() must_== new java.util.ArrayList(0)

      val sField = javaFieldtoScalaField(jField)
      sField.name must_== null
      sField.id must_== ""
      sField.label must_== ""
      sField.constraints must_== Nil
      sField.errors must_== Nil
    }

  }
}
