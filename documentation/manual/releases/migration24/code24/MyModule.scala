/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scaladoc {
package module {

import mycomponent._
//#module-decl
import play.api.Configuration
import play.api.Environment
import play.api.inject.Binding
import play.api.inject.Module

class MyModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[MyComponent].to[MyComponentImpl]
    )
  }
}
//#module-decl

//#components-decl
import play.api.inject.ApplicationLifecycle

trait MyComponents {
  def applicationLifecycle: ApplicationLifecycle
  lazy val component: MyComponent = new MyComponentImpl(applicationLifecycle)
}
//#components-decl
}
}