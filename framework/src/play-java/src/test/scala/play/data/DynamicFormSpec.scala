/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment }
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi }
import play.data.format.Formatters;
import views.html.helper.inputText
import play.core.j.PlayMagicForJava.javaFieldtoScalaField
import views.html.helper.FieldConstructor.defaultField
import scala.collection.JavaConversions._
import javax.validation.Validation

/**
 * Specs for Java dynamic forms
 */
object DynamicFormSpec extends Specification {
  val messagesApi = new DefaultMessagesApi(Environment.simple(), Configuration.reference, new DefaultLangs(Configuration.reference))
  implicit val messages = messagesApi.preferred(Seq.empty)
  val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
  val validator = Validation.buildDefaultValidatorFactory().getValidator()

  "a dynamic form" should {

    "bind values from a request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.get("foo") must_== "bar"
    }

    "allow access to raw data values from request" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.data().get("foo") must_== "bar"
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
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map("foo" -> Array("bar"))))
      form.reject("foo", "There was an error")
      val html = inputText(form("foo")).body
      html must contain("There was an error")
    }

    "display errors when a field is not present" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).bindFromRequest(FormSpec.dummyRequest(Map()))
      form.reject("foo", "Foo is required")
      val html = inputText(form("foo")).body
      html must contain("Foo is required")
    }

    "allow access to the property when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).fill(Map("foo" -> "bar"))
      form.get("foo") must_== "bar"
    }

    "allow access to the equivalent of the raw data when filled" in {
      val form = new DynamicForm(jMessagesApi, new Formatters(jMessagesApi), validator).fill(Map("foo" -> "bar"))
      form("foo").value() must_== "bar"
    }

  }
}
