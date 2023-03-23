/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scaladoc {
  package module {

    import mycomponent._
    import play.api.inject.Binding
    import play.api.inject.Module
//#module-decl
    import play.api.Configuration
    import play.api.Environment

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
