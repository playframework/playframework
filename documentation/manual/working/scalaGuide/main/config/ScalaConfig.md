<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# The Scala Configuration API

Play uses the [Typesafe config library](https://github.com/typesafehub/config), but Play also provides a nice Scala wrapper called [`Configuration`](api/scala/play/api/Configuration.html) with more advanced Scala features. If you're not familiar with Typesafe config, you may also want to read the documentation on [[configuration file syntax and features|ConfigFile]].

## Accessing the configuration

Typically, you'll obtain a `Configuration` object through [[Dependency Injection|ScalaDependencyInjection]], or simply by passing an instance of `Configuration` to your component:

@[inject-config](code/ScalaConfig.scala)

The `get` method is the most common one you'll use. This is used to get a single value at a path in the configuration file.

@[config-get](code/ScalaConfig.scala)

It accepts an implicit `ConfigLoader`, but for most common types like `String`, `Int`, and even `Seq[String]`, there are [already loaders defined](api/scala/play/api/ConfigLoader$.html) that do what you'd expect.

`Configuration` also supports validating against a set of valid values:

@[config-validate](code/ScalaConfig.scala)

### ConfigLoader

By defining your own [`ConfigLoader`](api/scala/play/api/ConfigLoader.html), you can easily convert configuration into a custom type. This is used extensively in Play internally, and is a great way to bring more type safety to your use of configuration. For example:

@[config-loader-example](code/ScalaConfig.scala)

Then you can use `config.get` as we did above:

@[config-loader-get](code/ScalaConfig.scala)

### Optional configuration keys

Play's `Configuration` supports getting optional configuration keys using the `getOptional[A]` method. It works just like `get[A]` but will return `None` if the key does not exist. Instead of using this method, we recommend setting optional keys to `null` in your configuration file and using `get[Option[A]]`. But we provide this method for convenience in case you need to interface with libraries that use configuration in a non-standard way.
