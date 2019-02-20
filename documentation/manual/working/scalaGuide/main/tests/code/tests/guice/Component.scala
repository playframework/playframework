/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.guice

// #component
trait Component {
  def hello: String
}

class DefaultComponent extends Component {
  def hello = "default"
}

class MockComponent extends Component {
  def hello = "mock"
}
// #component

// #component-module
import play.api.Environment
import play.api.Configuration
import play.api.inject.Module

class ComponentModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[Component].to[DefaultComponent]
  )
}
// #component-module
