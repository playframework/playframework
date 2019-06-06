/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.scalafieldconstructor {

  import org.specs2.mutable.Specification
  import play.api.http.HttpConfiguration
  import play.api.Configuration
  import play.api.Environment
  import play.api.i18n._

  class ScalaFieldConstructorSpec extends Specification {

    val environment                 = Environment.simple()
    val conf                        = Configuration.reference
    val langs                       = new DefaultLangsProvider(conf).get
    val httpConfiguration           = HttpConfiguration.fromConfiguration(conf, environment)
    val messagesApi                 = new DefaultMessagesApiProvider(environment, conf, langs, httpConfiguration).get
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

    val form = Form(
      mapping(
        "username" -> text
      )(User.apply)(User.unapply)
    )
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
    case class User(username: String)
  }

}
