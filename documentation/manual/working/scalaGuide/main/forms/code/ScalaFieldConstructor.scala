/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.scalafieldconstructor {

import org.specs2.mutable.Specification
import play.api.{Configuration, Environment}
import play.api.i18n._

class ScalaFieldConstructorSpec extends Specification {

  val conf = Configuration.reference
  val messagesApi = new DefaultMessagesApiProvider(Environment.simple(), conf, new DefaultLangsProvider(conf).get).get
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  "field constructors" should {

    "be possible to import" in {
      html.userImport(MyForm.form).body must contain("--foo--")
    }

    "be possible to declare" in {
      html.userDeclare(MyForm.form).body must contain("--foo--")
    }
  }
}

object MyForm {
  import play.api.data.Form
  import play.api.data.Forms._
  import html.models.User

  val form = Form(mapping(
    "username" -> text
  )(User.apply)(User.unapply))
}

package html {
//#form-myfield-helper
object MyHelpers {
  import views.html.helper.FieldConstructor
  implicit val myFields = FieldConstructor(html.myFieldConstructorTemplate.f)
}
//#form-myfield-helper

}

package html.models {
  case class User(username:String)
}

}
