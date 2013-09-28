/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.dependencyinjection

import play.api._
import play.api.mvc._

// #di-global
object Global extends play.api.GlobalSettings {

  private val injector = SomeDependencyInjectionFramework

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    injector.getInstance(controllerClass)
  }
}
// #di-global

/**
 * Simplest possible DI framework.
 */
object SomeDependencyInjectionFramework {
  def getInstance[A](controllerClass: Class[A]) : A = {
    controllerClass.newInstance()
  }
}
