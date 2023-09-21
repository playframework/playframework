/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.views

import play.api.http.DefaultHttpErrorHandler
import play.api.http.DevHttpErrorHandler
import play.api.test._

class DevErrorPageSpec extends PlaySpecification {
  "devError.scala.html" should {
    val testExceptionSource = new play.api.PlayException.ExceptionSource("test", "making sure the link shows up") {
      def line       = 100.asInstanceOf[Integer]
      def position   = 20.asInstanceOf[Integer]
      def input      = "test"
      def sourceName = "someSourceFile"
    }

    "link the error line if play.editor is configured" in {
      DevHttpErrorHandler.setPlayEditor("someEditorLinkWith %s:%s")
      val result = DevHttpErrorHandler.onServerError(FakeRequest(), testExceptionSource)
      contentAsString(result) must contain("""href="someEditorLinkWith someSourceFile:100" """)
    }

    "show prod error page in prod mode" in {
      val errorHandler = new DefaultHttpErrorHandler()
      val result       = errorHandler.onServerError(FakeRequest(), testExceptionSource)
      Helpers.contentAsString(result) must contain("Oops, an error occurred")
    }
  }
}
