package javaguide.forms

import org.specs2.mutable.Specification
import play.data.Form
import javaguide.forms.html.{UserForm, User}
import java.util

object JavaFormHelpers extends Specification {

  "java form helpers" should {
    {
      val form = Form.form(classOf[User])
      val u = new UserForm
      u.name = "foo"
      u.emails = util.Arrays.asList("a@a", "b@b")
      val userForm = Form.form(classOf[UserForm]).fill(u)
      val body = html.helpers(form, userForm).body
      def segment(name: String) = {
        body.lines.dropWhile(_ != "<span class=\"" + name + "\">").drop(1).takeWhile(_ != "</span>").mkString("\n")
      }

      "allow rendering a form" in {
        val form = segment("form")
        form must contain("<form")
        form must contain("""action="/form"""")
      }

      "allow rendering a form with an id" in {
        val form = segment("form-with-id")
        form must contain("<form")
        form must contain("""id="myForm"""")
      }

      "allow passing extra parameters to an input" in {
        val input = segment("extra-params")
        input must contain("""id="email"""")
        input must contain("""size="30"""")
      }

      "allow repeated form fields" in {
        val input = segment("repeat")
        input must contain("emails[0]")
        input must contain("emails[1]")
      }
    }

    {
      "allow rendering input fields" in {
        val form = Form.form(classOf[User])
        val body = html.fullform(form).body
        body must contain("""type="text"""")
        body must contain("""type="password"""")
        body must contain("""name="email"""")
        body must contain("""name="password"""")
      }

      "allow custom field constructors" in {
        val form = Form.form(classOf[User])
        val body = html.withFieldConstructor(form).body
        body must contain("foobar")
      }

    }

  }


}
