<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Testing with Guice

If you're using Guice for [[dependency injection|ScalaDependencyInjection]] then you can directly configure how components and applications are created for tests. This includes adding extra bindings or overriding existing bindings.

## GuiceApplicationBuilder

[GuiceApplicationBuilder](api/scala/index.html#play.api.inject.guice.GuiceApplicationBuilder) provides a builder API for configuring the dependency injection and creation of an [Application](api/scala/index.html#play.api.Application).

### Environment

The [Environment](api/scala/index.html#play.api.Environment), or parts of the environment such as the root path, mode, or class loader for an application, can be specified. The configured environment will be used for loading the application configuration, it will be used when loading modules and passed when deriving bindings from Play modules, and it will be injectable into other components.

@[builder-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[set-environment](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[set-environment-values](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

### Configuration

Additional configuration can be added. This configuration will always be in addition to the configuration loaded automatically for the application. When existing keys are used the new configuration will be preferred.

@[add-configuration](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

The automatic loading of configuration from the application environment can also be overridden. This will completely replace the application configuration. For example:

@[override-configuration](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

### Bindings and Modules

The bindings used for dependency injection are completely configurable. The builder methods support [[Play Modules and Bindings|ScalaDependencyInjection]] and also Guice Modules.

#### Additional bindings

Additional bindings, via Play modules, Play bindings, or Guice modules, can be added:

@[bind-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[add-bindings](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

#### Override bindings

Bindings can be overridden using Play bindings, or modules that provide  bindings. For example:

@[override-bindings](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

#### Disable modules

Any loaded modules can be disabled by class name:

@[disable-modules](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

#### Loaded modules

Modules are automatically loaded from the classpath based on the `play.modules.enabled` configuration. This default loading of modules can be overridden. For example:

@[load-modules](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)


## GuiceInjectorBuilder

[GuiceInjectorBuilder](api/scala/index.html#play.api.inject.guice.GuiceInjectorBuilder) provides a builder API for configuring Guice dependency injection more generally. This builder does not load configuration or modules automatically from the environment like `GuiceApplicationBuilder`, but provides a completely clean state for adding configuration and bindings. The common interface for both builders can be found in [GuiceBuilder](api/scala/index.html#play.api.inject.guice.GuiceBuilder). A Play [Injector](api/scala/index.html#play.api.inject.Injector) is created. Here's an example of instantiating a component using the injector builder:

@[injector-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[bind-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[injector-builder](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)


## Overriding bindings in a functional test

Here is a full example of replacing a component with a mock component for testing. Let's start with a component, that has a default implementation and a mock implementation for testing:

@[component](code/tests/guice/Component.scala)

This component is loaded automatically using a module:

@[component-module](code/tests/guice/Component.scala)

And the component is used in a controller:

@[controller](code/tests/guice/controllers/Application.scala)

To build an `Application` to use in functional tests we can simply override the binding for the component:

@[builder-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[bind-imports](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)
@[override-bindings](code/tests/guice/ScalaGuiceApplicationBuilderSpec.scala)

The created application can be used with the functional testing helpers for [[Specs2|ScalaFunctionalTestingWithSpecs2]] and [[ScalaTest|ScalaFunctionalTestingWithScalaTest]].
