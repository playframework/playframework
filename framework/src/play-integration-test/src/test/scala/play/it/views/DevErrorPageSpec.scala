package play.it.views

import play.api.{ Configuration, Mode, Environment }
import play.api.http.DefaultHttpErrorHandler
import play.api.test._

object DevErrorPageSpec extends PlaySpecification {

  "devError.scala.html" should {

    val testExceptionSource = new play.api.PlayException.ExceptionSource("test", "making sure the link shows up") {
      def line = 100.asInstanceOf[Integer]
      def position = 20.asInstanceOf[Integer]
      def input = "test"
      def sourceName = "someSourceFile"
    }

    "link the error line if play.editor is configured" in new WithApplication(FakeApplication(
      additionalConfiguration = Map("play.editor" -> "someEditorLinkWith %s:%s")
    )) {
      val result = app.errorHandler.onServerError(FakeRequest(), testExceptionSource)
      contentAsString(result) must contain("""href="someEditorLinkWith someSourceFile:100" """)
    }

    "show prod error page in prod mode" in {
      val errorHandler = new DefaultHttpErrorHandler(Environment.simple(mode = Mode.Prod), Configuration.empty)
      val result = errorHandler.onServerError(FakeRequest(), testExceptionSource)
      Helpers.contentAsString(result) must contain("Oops, an error occurred")
    }
  }

}
