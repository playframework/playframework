/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import org.specs2.mutable.Specification
import play.api.inject.{ Injector, NewInstanceInjector }
import play.api.mvc.{ EssentialAction, EssentialFilter }
import play.api.{ Configuration, Environment }

/**
 * Unit tests for default filter spec functionality
 */
class DefaultFiltersSpec extends Specification {

  "DefaultFilters" should {

    "work when defined" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map("play.filters.defaults.0" -> "play.api.http.MyTestFilter"))
      val injector: Injector = NewInstanceInjector
      val defaultFilters = new DefaultFilters(env, conf, injector)

      defaultFilters.filters must haveLength(1)
      defaultFilters.filters.head must beAnInstanceOf[MyTestFilter]
    }

    "work when set to null explicitly" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map("play.filters.defaults" -> null))
      val injector: Injector = NewInstanceInjector
      val defaultFilters = new DefaultFilters(env, conf, injector)

      defaultFilters.filters must haveLength(0)
    }

    "work when undefined" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map())
      val injector: Injector = NewInstanceInjector
      val defaultFilters = new DefaultFilters(env, conf, injector)

      defaultFilters.filters must haveLength(0)
    }

    "throw illegal state exception when using class that does not exist" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map("play.filters.defaults.0" -> "NoSuchFilter"))
      val injector: Injector = NewInstanceInjector

      {
        new DefaultFilters(env, conf, injector)
      } must throwAn[IllegalStateException]
    }
  }

  "DefaultHttpFilters" should {

    "work with the default filters and user filters constructor" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map())
      val injector: Injector = NewInstanceInjector
      val defaultFilters = new DefaultFilters(env, conf, injector)

      val filter = new MyTestFilter()
      val filters = new DefaultHttpFilters(defaultFilters, filter)
      filters.filters must haveLength(1)
    }

    "work with defaultFilters only in constructor" in {
      val env: Environment = Environment.simple()
      val conf: Configuration = Configuration.from(Map("play.filters.defaults.0" -> "play.api.http.MyTestFilter"))
      val injector: Injector = NewInstanceInjector
      val defaultFilters = new DefaultFilters(env, conf, injector)

      val defaultHttpFilters = new DefaultHttpFilters(defaultFilters) // added to the default filter
      defaultHttpFilters.filters must haveLength(1)
      defaultHttpFilters.filters.head must beAnInstanceOf[MyTestFilter]
    }

    "work with the deprecated constructor that does not take defaultFilters" in {
      val filter = new MyTestFilter()
      val filters = new DefaultHttpFilters(filter)
      filters.filters must haveLength(1)
    }
  }

}

class MyTestFilter extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = ???
}
