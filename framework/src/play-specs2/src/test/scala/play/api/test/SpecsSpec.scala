/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import com.google.inject.AbstractModule
import org.specs2.mutable._
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }
import play.api.{ Play, Application }

object SpecsSpec extends Specification {

  def fakeApp[A](elems: (String, String)*) = FakeApplication(additionalConfiguration = Map(elems: _*))
  def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)

  "WithApplication context" should {
    "provide an app" in new WithApplication(fakeApp("foo" -> "bar", "ehcacheplugin" -> "disabled")) {
      app.configuration.getString("foo") must beSome("bar")
    }
    "make the app available implicitly" in new WithApplication(fakeApp("foo" -> "bar", "ehcacheplugin" -> "disabled")) {
      getConfig("foo") must beSome("bar")
    }
    "start the application" in new WithApplication(fakeApp("foo" -> "bar", "ehcacheplugin" -> "disabled")) {
      Play.maybeApplication must beSome(app)
    }
  }

  "WithApplicationLoader" should {
    val myModule = new AbstractModule {
      def configure() = bind(classOf[Int]).toInstance(42)
    }
    val builder = new GuiceApplicationBuilder().bindings(myModule)
    class WithMyApplicationLoader extends WithApplicationLoader(new GuiceApplicationLoader(builder))
    "allow adding modules" in new WithMyApplicationLoader {
      app.injector.instanceOf(classOf[Int]) must equalTo(42)
    }
  }
}
