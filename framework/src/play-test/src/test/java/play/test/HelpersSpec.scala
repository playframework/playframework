package play.test

import org.specs2.mutable._
import play.mvc.Result
import scala.concurrent.Future
import play.api.mvc.{Cookie, Results, SimpleResult}

/**
 *
 */
object HelpersSpec extends Specification {

  "Helpers" should {

    // This is in Scala because building wrapped scala results is easier.
    "test for cookies" in {

      val javaResult: play.mvc.Result = new Result() {
        def getWrappedResult: Future[SimpleResult] = {
          import scala.concurrent.ExecutionContext.Implicits.global
          Future {
            Results.Ok("Hello world").withCookies(Cookie("name1", "value1"))
          }
        }
      }

      val cookies = Helpers.cookies(javaResult)
      val cookie = cookies.iterator().next()

      cookie.name() must be_==("name1")
      cookie.value() must be_==("value1")
    }
  }
}
