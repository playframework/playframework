package play.it.views

import play.api.test.PlaySpecification

import play.api.test._
import play.api.DefaultGlobal
import java.lang.String
import scala.Predef.String

object DevErrorPageSpec extends PlaySpecification{


  "devError.scala.html" should {

    val testExceptionSource = new play.api.PlayException.ExceptionSource("test","making sure the link shows up") {
      def line = 100.asInstanceOf[Integer]
      def position = 20.asInstanceOf[Integer]
      def input = "test"
      def sourceName = "someSourceFile"
    }

    "link the error line if play.editor is configured" in {
        try {
          System.setProperty("play.editor", "someEditorLinkWith %s:%s")
          val result = DefaultGlobal.onError(FakeRequest(), testExceptionSource)
          Helpers.contentAsString(result) must contain("""href="someEditorLinkWith someSourceFile:100" """)
        } finally {
          System.clearProperty("play.editor")
        }
    }

    "show prod error page in prod mode" in {
      val fakeApplication = new FakeApplication() {
        override val mode = play.api.Mode.Prod
      }
      running(fakeApplication) {
        val result = DefaultGlobal.onError(FakeRequest(), testExceptionSource)
        Helpers.contentAsString(result) must contain("Oops, an error occured")
      }
    }
  }

}