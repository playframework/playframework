/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
import play.api.inject.Binding
import play.api.Environment
import play.api.Configuration
import play.api.inject.Module

class ComponentModule extends Module {
  def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = Seq(
    bind[Component].to[DefaultComponent]
  )
}
// #component-module
