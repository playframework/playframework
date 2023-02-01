/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import scala.language.reflectiveCalls

import org.mockito.Mockito
import org.specs2.mutable._
import play.api.inject.Injector
import play.api.Application

class InjectingSpec extends Specification {
  class Foo

  class AppContainer(val app: Application)

  "Injecting trait" should {
    "provide an instance when asked for a class" in {
      val injector = Mockito.mock(classOf[Injector])
      val app      = Mockito.mock(classOf[Application])
      Mockito.when(app.injector).thenReturn(injector)
      val expected = new Foo
      Mockito.when(injector.instanceOf[Foo]).thenReturn(expected)

      val appContainer = new AppContainer(app) with Injecting
      val actual: Foo  = appContainer.inject[Foo]
      actual must_== expected
    }
  }
}
