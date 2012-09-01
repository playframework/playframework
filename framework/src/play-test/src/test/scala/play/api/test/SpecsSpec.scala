package play.api.test

import org.specs2.mutable._
import play.api.{Play, Application}

object SpecsSpec extends Specification {

  def fakeApp[A](elems: (String, String)*) = FakeApplication(additionalConfiguration = Map(elems:_*))
  def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)

  "WithApplication context" should {
    "provide an app" in new WithApplication(fakeApp("foo" -> "bar")) {
      app.configuration.getString("foo") must beSome("bar")
    }
    "make the app available implicitly" in new WithApplication(fakeApp("foo" -> "bar")) {
      getConfig("foo") must_== "bar"
    }
    "start the application" in new WithApplication(fakeApp("foo" -> "bar")) {
      Play.maybeApplication must beSome(app)
    }
  }
}
