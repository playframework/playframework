/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.Application
import play.api.inject.Injector

import scala.language.reflectiveCalls

class InjectingSpec extends Specification with Mockito with Injectable {

  class Foo

  class AppContainer(val app: Application)

  "Injecting trait" should {
    "provide an instance when using inject" in {
      val injector = mock[Injector]
      val app = mock[Application]
      app.injector returns injector
      val expected = new Foo
      injector.instanceOf[Foo] returns expected

      val appContainer = new AppContainer(app) with Injecting
      val actual: Foo = appContainer.inject[Foo]
      actual must_== expected
    }

    "provide an instance when using injecting" in {
      val injector = mock[Injector]
      val app = mock[Application]
      app.injector returns injector
      val expected = new Foo
      injector.instanceOf[Foo] returns expected

      val appContainer = new AppContainer(app) with Injecting
      val actual: Foo = appContainer.injecting { foo: Foo => foo }
      actual must_== expected
    }
  }

  "Injectable trait" should {
    "provide an instance when using inject" in {
      val injector = mock[Injector]
      implicit val app = mock[Application] // must be implicit
      app.injector returns injector
      val expected = new Foo
      injector.instanceOf[Foo] returns expected

      val actual: Foo = inject[Foo]
      actual must_== expected
    }

    "provide an instance when using injecting" in {
      val injector = mock[Injector]
      implicit val app = mock[Application] // must be implicit
      app.injector returns injector
      val expected = new Foo
      injector.instanceOf[Foo] returns expected

      val actual: Foo = injecting { foo: Foo => foo }
      actual must_== expected
    }
  }
}
