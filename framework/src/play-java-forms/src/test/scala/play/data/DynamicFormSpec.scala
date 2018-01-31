/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data

import javax.validation.Validation

import org.specs2.mutable.Specification
import play.api.i18n.DefaultMessagesApi
import play.core.j.PlayFormsMagicForJava.javaFieldtoScalaField
import play.data.format.Formatters
import views.html.helper.FieldConstructor.defaultField
import views.html.helper.inputText

import scala.collection.JavaConverters._

/**
 * Specs for Java dynamic forms
 */
class DynamicFormSpec extends Specification {

  val messagesApi = new DefaultMessagesApi()
  implicit val messages = messagesApi.preferred(Seq.empty)
  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
  val validator = FormSpec.validator()

  "a dynamic form" should {

    "bind values from a request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.get("foo") must_== "bar"
      form.value("foo").get must_== "bar"
    }

    "allow access to raw data values from request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.rawData().get("foo") must_== "bar"
    }

    "display submitted values in template helpers" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      val html = inputText(form("foo")).body
      html must contain("value=\"bar\"")
      html must contain("name=\"foo\"")
    }

    "render correctly when no value is submitted in template helpers" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map()))
      val html = inputText(form("foo")).body
      html must contain("value=\"\"")
      html must contain("name=\"foo\"")
    }

    "display errors in template helpers" in {
      var form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form = form.withError("foo", "There was an error")
      val html = inputText(form("foo")).body
      html must contain("There was an error")
    }

    "display errors when a field is not present" in {
      var form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map()))
      form = form.withError("foo", "Foo is required")
      val html = inputText(form("foo")).body
      html must contain("Foo is required")
    }

    "allow access to the property when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form.get("foo") must_== "bar"
      form.value("foo").get must_== "bar"
    }

    "allow access to the equivalent of the raw data when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form("foo").value().get() must_== "bar"
    }

    "don't throw NullPointerException when all components of form are null" in {
      val form = new DynamicForm(null, null, null).fill(Map("foo" -> "bar").asInstanceOf[Map[String, Object]].asJava)
      form("foo").value().get() must_== "bar"
    }

    "convert jField to scala Field when all components of jField are null" in {
      val jField = new play.data.Form.Field(null, null, null, null, null, null)
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
