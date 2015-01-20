/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms

import play.api.test.{WithApplication, PlaySpecification}
import play.data.Form
import javaguide.forms.html.{UserForm, User}
import java.util

object JavaFormHelpers extends PlaySpecification {

  "java form helpers" should {
    {
      val form = Form.form(classOf[User])
      val u = new UserForm
      u.name = "foo"
      u.emails = util.Arrays.asList("a@a", "b@b")
      val userForm = Form.form(classOf[UserForm]).fill(u)
      def segment(name: String) = {
        val body = html.helpers(form, userForm).body
        body.lines.dropWhile(_ != "<span class=\"" + name + "\">").drop(1).takeWhile(_ != "</span>").mkString("\n")
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
        val form = Form.form(classOf[User])
        val body = html.fullform(form).body
        body must contain("""type="text"""")
        body must contain("""type="password"""")
        body must contain("""name="email"""")
        body must contain("""name="password"""")
      }

      "allow custom field constructors" in new WithApplication() {
        val form = Form.form(classOf[User])
        val body = html.withFieldConstructor(form).body
        body must contain("foobar")
      }

    }

  }


}
