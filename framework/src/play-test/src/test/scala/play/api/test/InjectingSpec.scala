/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.Application
import play.api.inject.Injector

import scala.language.reflectiveCalls

class InjectingSpec extends Specification with Mockito {

  class Foo

  class AppContainer(val app: Application)

  "Injecting trait" should {

    "provide an instance when asked for a class" in {
      val injector = mock[Injector]
      val app = mock[Application]
      app.injector returns injector
      val expected = new Foo
      injector.instanceOf[Foo] returns expected

      val appContainer = new AppContainer(app) with Injecting
      val actual: Foo = appContainer.inject[Foo]
      actual must_== expected
    }
  }
}
