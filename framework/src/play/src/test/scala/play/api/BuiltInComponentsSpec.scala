/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io.File
import java.net.URLClassLoader

import org.specs2.mutable.Specification
import play.api.inject.DefaultApplicationLifecycle
import play.api.mvc.EssentialFilter
import play.api.routing.Router

class BuiltInComponentsSpec extends Specification {
  "BuiltinComponents" should {
    "use the Environment ClassLoader for runtime injection" in {
      val classLoader = new URLClassLoader(Array())
      val components = new BuiltInComponents {
        override val environment: Environment = Environment(new File("."), classLoader, Mode.Test)
        override def configuration: Configuration = Configuration.load(environment)
        override def applicationLifecycle: DefaultApplicationLifecycle = new DefaultApplicationLifecycle
        override def router: Router = ???
        override def httpFilters: Seq[EssentialFilter] = ???
      }
      components.environment.classLoader must_== classLoader
      val constructedObject = components.injector.instanceOf[BuiltInComponentsSpec.ClassLoaderAware]
      constructedObject.constructionClassLoader must_== classLoader
    }
  }
}

object BuiltInComponentsSpec {
  class ClassLoaderAware {
    // This is the value of the Thread's context ClassLoader at the time the object is constructed
    val constructionClassLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  }
}