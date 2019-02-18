/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.i18n.scalai18n {

  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api._
  import play.api.http.HttpConfiguration
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.mvc._
  import play.api.test._

  package views.html {

    object formpage {
      def apply()(implicit messages: play.api.i18n.Messages): String = {
        ""
      }
    }

  }

  //#i18n-messagescontroller
  import javax.inject.Inject
  import play.api.i18n._

  class MyMessagesController @Inject()(mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {

    def index = Action { implicit request: MessagesRequest[AnyContent] =>
      val messages: Messages = request.messages
      val message: String    = messages("info.error")
      Ok(message)
    }

    def messages2 = Action { implicit request: MessagesRequest[AnyContent] =>
      val lang: Lang      = request.messages.lang
      val message: String = messagesApi("info.error")(lang)
      Ok(message)
    }

    def messages4 = Action { implicit request: MessagesRequest[AnyContent] =>
      // MessagesRequest is an implicit MessagesProvider
      Ok(views.html.formpage())
    }
  }
  //#i18n-messagescontroller

  //#i18n-support
  import javax.inject.Inject
  import play.api.i18n._

  class MySupportController @Inject()(val controllerComponents: ControllerComponents)
      extends BaseController
      with I18nSupport {

    def index = Action { implicit request =>
      // type enrichment through I18nSupport
      val messages: Messages = request.messages
      val message: String    = messages("info.error")
      Ok(message)
    }

    def messages2 = Action { implicit request =>
      // type enrichment through I18nSupport
      val lang: Lang      = request.lang
      val message: String = messagesApi("info.error")(lang)
      Ok(message)
    }

    def messages3 = Action { request =>
      // direct access with no implicits required
      val messages: Messages = messagesApi.preferred(request)
      val lang               = messages.lang
      val message: String    = messages("info.error")
      Ok(message)
    }

    def messages4 = Action { implicit request =>
      // takes implicit Messages, converted using request2messages
      // template defined with @()(implicit messages: Messages)
      Ok(views.html.formpage())
    }
  }
  //#i18n-support

  @RunWith(classOf[JUnitRunner])
  class ScalaI18nSpec extends AbstractController(Helpers.stubControllerComponents()) with PlaySpecification {
    val conf = Configuration.reference ++ Configuration.from(Map("play.i18n.path" -> "scalaguide/i18n"))

    "An i18nsupport controller" should {

      "return the right message" in new WithApplication(GuiceApplicationBuilder().loadConfig(conf).build()) {
        val controller = app.injector.instanceOf[MySupportController]

        val result = controller.index(FakeRequest())
        contentAsString(result) must contain("You aren't logged in!")
      }
    }

    "An messages controller" should {

      "return the right message" in new WithApplication(GuiceApplicationBuilder().loadConfig(conf).build()) {
        val controller = app.injector.instanceOf[MyMessagesController]

        val result = controller.index(FakeRequest())
        contentAsString(result) must contain("You aren't logged in!")
      }
    }

    "A Scala translation" should {

      val env               = Environment.simple()
      val langs             = new DefaultLangsProvider(conf).get
      val httpConfiguration = HttpConfiguration.fromConfiguration(conf, env)
      val messagesApi       = new DefaultMessagesApiProvider(env, conf, langs, httpConfiguration).get

      implicit val lang = Lang("en")

      "escape single quotes" in {
        //#apostrophe-messages
        messagesApi("info.error") == "You aren't logged in!"
        //#apostrophe-messages
      }

      "escape parameter substitution" in {
        //#parameter-escaping
        messagesApi("example.formatting") == "When using MessageFormat, '{0}' is replaced with the first parameter."
        //#parameter-escaping
      }
    }

  }

}
