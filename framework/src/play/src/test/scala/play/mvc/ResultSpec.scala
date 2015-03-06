package play.test

import org.specs2.mutable._
import play.mvc.Result
import scala.concurrent.Future
import play.api.mvc.{ Cookie, Results, Result => ScalaResult }

/**
 *
 */
object ResultSpec extends Specification {

  "Result" should {

    // This is in Scala because building wrapped scala results is easier.
    "test for cookies" in {

      val javaResult: play.mvc.Result = new Result() {
        def toScala: ScalaResult = {
          Results.Ok("Hello world").withCookies(Cookie("name1", "value1"))
        }
      }

      val cookies = javaResult.cookies()
      val cookie = cookies.iterator().next()

      cookie.name() must be_==("name1")
      cookie.value() must be_==("value1")
    }
  }
}
