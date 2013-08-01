package scalaguide.async.scalacomet

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.{ defaultContext => dec }
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import play.api.test._
import scala.concurrent.Future
import play.api.libs.Comet

object ScalaCometSpec extends PlaySpecification with Controller {

  "play comet" should {

    "allow manually sending comet messages" in new WithApplication() {
      // todo note to self: Make sure I get rid of the Enumerator.eof once that bug is fixed
      //#manual
      def comet = Action {
        val events = Enumerator(
          """<script>console.log('kiki')</script>""",
          """<script>console.log('foo')</script>""",
          """<script>console.log('bar')</script>"""
        )
        Ok.stream(events >>> Enumerator.eof).as(HTML)
      }
      //#manual
      val msgs = cometMessages(comet(FakeRequest()))
      msgs must haveLength(3)
      msgs(0) must_== """<script>console.log('kiki')</script>"""
    }

    "allow a smarter way of manually sending comet messages" in new WithApplication() {
      //#enumeratee
      import play.api.templates.Html

      // Transform a String message into an Html script tag
      val toCometMessage = Enumeratee.map[String] { data =>
        Html("""<script>console.log('""" + data + """')</script>""")
      }

      def comet = Action {
        val events = Enumerator("kiki", "foo", "bar")
        Ok.stream((events &> toCometMessage) >>> Enumerator.eof)
      }
      //#enumeratee

      val msgs = cometMessages(comet(FakeRequest()))
      msgs must haveLength(3)
      msgs(0) must_== """<script>console.log('kiki')</script>"""
    }

    "allow using the comet helper" in new WithApplication() {
      //#helper
      def comet = Action {
        val events = Enumerator("kiki", "foo", "bar")
        Ok.stream((events &> Comet(callback = "console.log")) >>> Enumerator.eof)
      }
      //#helper

      val msgs = cometMessages(comet(FakeRequest()))
      msgs must haveLength(4)
      msgs(1) must contain("console.log('kiki')")
    }

    "allow using a forever iframe" in new WithApplication() {
      //#iframe
      def comet = Action {
        val events = Enumerator("kiki", "foo", "bar")
        Ok.stream((events &> Comet(callback = "parent.cometMessage")) >>> Enumerator.eof)
      }
      //#iframe

      val msgs = cometMessages(comet(FakeRequest()))
      msgs must haveLength(4)
      msgs(1) must contain("parent.cometMessage('kiki')")
    }

  }

  def cometMessages(result: Future[SimpleResult]):Seq[String] = {
    await(await(result).body &> Results.dechunk |>>> Iteratee.getChunks).map(bytes => new String(bytes))
  }
}
