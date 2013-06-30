package scalaguide.forms.scalaformhelper {

import play.api.mvc._
import play.api.test._

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ScalaFormHelperSpec extends Specification with Controller {

  "A scala forms helper" should {

    val loginForm = controllers.Application.loginForm
    val userForm = controllers.Application.userForm

    "generate from form helper contain html id" in {

      val login = scalaguide.forms.scalaformhelper.views.html.login(loginForm).body

      login must contain("""id="myForm"""")

    }

    "generate from user form helper contain html id" in {

      val login = scalaguide.forms.scalaformhelper.views.html.user(userForm).body

      login must contain("""id="username"""")

      login must contain("""password_field""")

    }



  }
}

package controllers {

import play.api.data.Form
import play.api.data.Forms._
import models.User

object Application extends Controller {

  def submit = Action {
    Ok("Welcome")
  }

  def home = Action{
    Ok("Welcome!")
  }

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    )
  )

  val userForm = Form(
    mapping(
      "name" -> text,
      "password" -> text
    )(User.apply)(User.unapply)
  )


}

}

}

package models {
  case class User(name:String, password:String)
}

package controllers {

import javaguide.forms.html.myFieldConstructorTemplate
import views.html.helper.FieldConstructor

//#form-myfield-helper
object MyHelpers {

  implicit val myFields = FieldConstructor(myFieldConstructorTemplate.f)

}
//#form-myfield-helper


}