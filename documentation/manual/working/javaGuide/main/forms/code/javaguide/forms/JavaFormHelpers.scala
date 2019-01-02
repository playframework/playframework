/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms

import play.api.Application
import play.api.test.{PlaySpecification, WithApplication}
import javaguide.forms.html.{User, UserForm}

import java.util

import play.mvc.Http

class JavaFormHelpers extends PlaySpecification {

  "java form helpers" should {
    def withFormFactory[A](block: (play.data.FormFactory, play.i18n.Messages) => A)(implicit app: Application): A = {
      val requestBuilder = new Http.RequestBuilder()
      val request = requestBuilder.build()
      val formFactory = app.injector.instanceOf[play.data.FormFactory]
      val messagesApi = app.injector.instanceOf[play.i18n.MessagesApi]
      block(formFactory, messagesApi.preferred(request))
    }
    {
      def segment(name: String)(implicit app: Application) = {
        withFormFactory { (formFactory: play.data.FormFactory, messages: play.i18n.Messages) =>
          val form = formFactory.form(classOf[User])
          val u = new UserForm
          u.setName("foo")
          u.setEmails(util.Arrays.asList("a@a", "b@b"))
          val userForm = formFactory.form(classOf[UserForm]).fill(u)
          val body = html.helpers(form, userForm)(messages).body
          body.linesIterator.dropWhile(_ != "<span class=\"" + name + "\">").drop(1).takeWhile(_ != "</span>").mkString("\n")
        }
      }

      "allow rendering a form" in new WithApplication() {
        val form = segment("form")
        form must contain("<form")
        form must contain("""action="/form"""")
      }

      "allow rendering a form with an id" in new WithApplication() {
        val form = segment("form-with-id")
        form must contain("<form")
        form must contain("""id="myForm"""")
      }

      "allow passing extra parameters to an input" in new WithApplication() {
        val input = segment("extra-params")
        input must contain("""id="email"""")
        input must contain("""size="30"""")
      }

      "allow repeated form fields" in new WithApplication() {
        val input = segment("repeat")
        input must contain("emails.0")
        input must contain("emails.1")
      }
    }

    {
      "allow rendering input fields" in new WithApplication() {
        withFormFactory { (formFactory: play.data.FormFactory, messages: play.i18n.Messages) =>
          val form = formFactory.form(classOf[User])
          val body = html.fullform(form)(messages).body
          body must contain("""type="text"""")
          body must contain("""type="password"""")
          body must contain("""name="email"""")
          body must contain("""name="password"""")
        }
      }

      "allow custom field constructors" in new WithApplication() {
        withFormFactory { (formFactory: play.data.FormFactory, messages: play.i18n.Messages) =>
          val form = formFactory.form(classOf[User])
          val body = html.withFieldConstructor(form)(messages).body
          body must contain("foobar")
        }
      }

    }

  }


}
