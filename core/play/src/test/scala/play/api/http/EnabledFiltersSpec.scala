/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import org.specs2.mutable.Specification
import play.api.inject.Injector
import play.api.inject.NewInstanceInjector
import play.api.mvc.EssentialAction
import play.api.mvc.EssentialFilter
import play.api.Configuration
import play.api.Environment
import play.api.PlayException

/**
 * Unit tests for default filter spec functionality
 */
class EnabledFiltersSpec extends Specification {
  "EnabledFilters" should {
    "work when defined" in {
      val env: Environment    = Environment.simple()
      val conf: Configuration = Configuration.from(
        Map(
          "play.filters.enabled.0"  -> "play.api.http.MyTestFilter",
          "play.filters.disabled.0" -> ""
        )
      )
      val injector: Injector = NewInstanceInjector
      val defaultFilters     = new EnabledFilters(env, conf, injector)

      defaultFilters.filters must haveLength(1)
      defaultFilters.filters.head must beAnInstanceOf[MyTestFilter]
    }

    "work when set to null explicitly" in {
      val env: Environment    = Environment.simple()
      val conf: Configuration = Configuration.from(Map("play.filters.enabled" -> null))
      val injector: Injector  = NewInstanceInjector
      val defaultFilters      = new EnabledFilters(env, conf, injector)

      defaultFilters.filters must haveLength(0)
    }

    "work when undefined" in {
      val env: Environment    = Environment.simple()
      val conf: Configuration = Configuration.from(Map())
      val injector: Injector  = NewInstanceInjector
      val defaultFilters      = new EnabledFilters(env, conf, injector)

      defaultFilters.filters must haveLength(0)
    }

    "throw config exception when using class that does not exist" in {
      val env: Environment    = Environment.simple()
      val conf: Configuration = Configuration.from(
        Map(
          "play.filters.enabled.0"  -> "NoSuchFilter",
          "play.filters.disabled.0" -> ""
        )
      )
      val injector: Injector = NewInstanceInjector

      {
        new EnabledFilters(env, conf, injector)
      } must throwAn[PlayException.ExceptionSource]
    }

    "work with disabled filter" in {
      val env: Environment    = Environment.simple()
      val conf: Configuration = Configuration.from(
        Map(
          "play.filters.enabled.0"  -> "play.api.http.MyTestFilter",
          "play.filters.enabled.1"  -> "play.api.http.MyTestFilter2",
          "play.filters.disabled.0" -> "play.api.http.MyTestFilter"
        )
      )
      val injector: Injector = NewInstanceInjector
      val defaultFilters     = new EnabledFilters(env, conf, injector)

      defaultFilters.filters must haveLength(1)
      defaultFilters.filters.head must beAnInstanceOf[MyTestFilter2]
    }
  }
}

class MyTestFilter extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = ???
}

class MyTestFilter2 extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = ???
}
