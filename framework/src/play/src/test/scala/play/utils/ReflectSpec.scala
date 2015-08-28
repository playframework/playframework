/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import javax.inject.Inject

import org.specs2.mutable.Specification
import play.api.inject.Binding
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.{ PlayConfig, PlayException, Configuration, Environment }

import scala.reflect.ClassTag

object ReflectSpec extends Specification {

  "Reflect" should {

    "load bindings from configuration" in {

      "return no bindings for provided configuration" in {
        bindings("provided", "none") must beEmpty
      }

      "return the default implementation when none configured or default class doesn't exist" in {
        doQuack(bindings(null, "NoDuck")) must_== "quack"
      }

      "return a default Scala implementation" in {
        doQuack(bindings[CustomDuck](null)) must_== "custom quack"
      }

      "return a default Java implementation" in {
        doQuack(bindings[CustomJavaDuck](null)) must_== "java quack"
      }

      "return a configured Scala implementation" in {
        doQuack(bindings(classOf[CustomDuck].getName, "NoDuck")) must_== "custom quack"
      }

      "return a configured Java implementation" in {
        doQuack(bindings(classOf[CustomJavaDuck].getName, "NoDuck")) must_== "java quack"
      }

      "throw an exception if a configured class doesn't exist" in {
        doQuack(bindings[CustomDuck]("NoDuck")) must throwA[PlayException]
      }

      "throw an exception if a configured class doesn't implement either of the interfaces" in {
        doQuack(bindings[CustomDuck](classOf[NotADuck].getName)) must throwA[PlayException]
      }

    }
  }

  def bindings(configured: String, defaultClassName: String): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[Duck, JavaDuck, JavaDuckAdapter, JavaDuckDelegate, DefaultDuck](
      Environment.simple(), PlayConfig(Configuration.from(Map("duck" -> configured))), "duck", defaultClassName)
  }

  def bindings[Default: ClassTag](configured: String): Seq[Binding[_]] = {
    bindings(configured, implicitly[ClassTag[Default]].runtimeClass.getName)
  }

  trait Duck {
    def quack: String
  }

  trait JavaDuck {
    def getQuack: String
  }

  class JavaDuckAdapter @Inject() (underlying: JavaDuck) extends Duck {
    def quack = underlying.getQuack
  }

  class DefaultDuck extends Duck {
    def quack = "quack"
  }

  class CustomDuck extends Duck {
    def quack = "custom quack"
  }

  class CustomJavaDuck extends JavaDuck {
    def getQuack = "java quack"
  }

  class JavaDuckDelegate @Inject() (delegate: Duck) extends JavaDuck {
    def getQuack = delegate.quack
  }

  class NotADuck

  def doQuack(bindings: Seq[Binding[_]]): String = {
    val injector = new GuiceInjectorBuilder().bindings(bindings).injector
    val duck = injector.instanceOf[Duck]
    val javaDuck = injector.instanceOf[JavaDuck]

    // The Java duck and the Scala duck must agree
    javaDuck.getQuack must_== duck.quack

    duck.quack
  }

}
