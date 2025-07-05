/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import org.specs2.matcher.BeEqualTypedValueCheck
import org.specs2.mutable.Specification
import play.{ Environment => JavaEnvironment }
import play.api.Configuration
import play.api.Environment

class ModulesSpec extends Specification {
  "Modules.locate" should {
    "load simple Guice modules" in {
      val env  = Environment.simple()
      val conf = Configuration(
        "play.modules.enabled" -> Seq(
          classOf[PlainGuiceModule].getName
        )
      )

      val located: Seq[AnyRef] = Modules.locate(env, conf)
      located.size must_== 1

      val head = located.head.asInstanceOf[BeEqualTypedValueCheck[AnyRef]]
      head.expected must beAnInstanceOf[PlainGuiceModule]
    }

    "load Guice modules that take a Scala Environment and Configuration" in {
      val env  = Environment.simple()
      val conf = Configuration(
        "play.modules.enabled" -> Seq(
          classOf[ScalaGuiceModule].getName
        )
      )
      val located: Seq[Any] = Modules.locate(env, conf)
      located.size must_== 1
      located.head must beLike {
        case mod: ScalaGuiceModule =>
          mod.environment must_== env
          mod.configuration must_== conf
      }
    }

    "load Guice modules that take a Java Environment and Config" in {
      val env  = Environment.simple()
      val conf = Configuration(
        "play.modules.enabled" -> Seq(
          classOf[JavaGuiceConfigModule].getName
        )
      )
      val located: Seq[Any] = Modules.locate(env, conf)
      located.size must_== 1
      located.head must beLike {
        case mod: JavaGuiceConfigModule =>
          mod.environment.asScala() must_== env
          mod.config must_== conf.underlying
      }
    }

    "load Guice modules that take a Config" in {
      val env  = Environment.simple()
      val conf = Configuration(
        "play.modules.enabled" -> Seq(
          classOf[GuiceConfigModule].getName
        )
      )
      val located: Seq[Any] = Modules.locate(env, conf)
      located.size must_== 1
      located.head must beLike {
        case mod: GuiceConfigModule =>
          mod.config must_== conf.underlying
      }
    }
  }
}

class PlainGuiceModule extends AbstractModule {
  override def configure(): Unit = ()
}

class ScalaGuiceModule(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = ()
}

class JavaGuiceConfigModule(val environment: JavaEnvironment, val config: Config) extends AbstractModule {
  override def configure(): Unit = ()
}

class GuiceConfigModule(val config: Config) extends AbstractModule {
  override def configure(): Unit = ()
}
