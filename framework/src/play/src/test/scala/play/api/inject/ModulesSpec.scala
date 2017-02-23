/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject

import org.specs2.matcher.BeEqualTypedValueCheck
import org.specs2.mutable.Specification

import com.google.inject.AbstractModule

import play.{ Configuration => JavaConfiguration, Environment => JavaEnvironment }
import play.api.{ Configuration, Environment }

class ModulesSpec extends Specification {

  "Modules.locate" should {

    "load simple Guice modules" in {
      val env = Environment.simple()
      val conf = Configuration("play.modules.enabled" -> Seq(
        classOf[PlainGuiceModule].getName
      ))

      val located: Seq[AnyRef] = Modules.locate(env, conf)
      located.size must_== 1

      val head = located.head.asInstanceOf[BeEqualTypedValueCheck[AnyRef]]
      head.expected must beAnInstanceOf[PlainGuiceModule]
    }

    "load Guice modules that take a Scala Environment and Configuration" in {
      val env = Environment.simple()
      val conf = Configuration("play.modules.enabled" -> Seq(
        classOf[ScalaGuiceModule].getName
      ))
      val located: Seq[Any] = Modules.locate(env, conf)
      located.size must_== 1
      located.head must beLike {
        case mod: ScalaGuiceModule =>
          mod.environment must_== env
          mod.configuration must_== conf
      }
    }

    "load Guice modules that take a Java Environment and Configuration" in {
      val env = Environment.simple()
      val conf = Configuration("play.modules.enabled" -> Seq(
        classOf[JavaGuiceModule].getName
      ))
      val located: Seq[Any] = Modules.locate(env, conf)
      located.size must_== 1
      located.head must beLike {
        case mod: JavaGuiceModule =>
          mod.environment.underlying must_== env
          mod.configuration.underlying must_== conf.underlying
      }
    }

  }

}

class PlainGuiceModule extends AbstractModule {
  def configure(): Unit = ()
}

class ScalaGuiceModule(
    val environment: Environment,
    val configuration: Configuration) extends AbstractModule {
  def configure(): Unit = ()
}

class JavaGuiceModule(
    val environment: JavaEnvironment,
    val configuration: JavaConfiguration) extends AbstractModule {
  def configure(): Unit = ()
}
