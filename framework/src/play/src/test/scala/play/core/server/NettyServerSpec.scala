package play.core.server

import org.specs2.mutable.Specification
import play.core.{ApplicationProvider}
import java.io.File
import play.api.Application
import play.api.mvc.Result

object NettyServerSpec extends Specification {

  class Fake extends ApplicationProvider {
    def path: File = new File(".")
    def get: Either[Throwable, Application] = Left(new RuntimeException)
  }

  "NettyServer" should {
    "fail when no https.port and http.port is missing" in {
      new NettyServer(
        new Fake,
        None,
        None
      ) must throwAn[IllegalArgumentException]
    }
  }
}
