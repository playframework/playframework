<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
#  Embedding an Akka Http server in your application

While Play apps are most commonly used as their own container, you can also embed a Play server into your own existing application. This can be used in conjunction with the Twirl template compiler and Play routes compiler, but these are of course not necessary. A common use case is an application with only a few simple routes. To use Akka HTTP Server embedded, you will need the following dependency:

@[akka-http-sbt-dependencies](code/embedded.sbt)

One way start a Play Akka HTTP Server is to use the [`AkkaHttpServer`](api/scala/play/core/server/AkkaHttpServer$.html) factory methods. If all you need to do is provide some straightforward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouterWithComponents` method:

@[simple-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/play/core/server/ServerConfig.html):

@[config-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

Play also provides components traits that make it easy to customize other components besides the router. The [`AkkaHttpServerComponents`](api/scala/play/core/server/AkkaHttpServerComponents.html) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to build the application that it requires. In this example we use [`DefaultAkkaHttpServerComponents`](api/scala/play/core/server/DefaultAkkaHttpServerComponents.html), which is equivalent to `AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents`:

@[components-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

Here the only method you need to implement is `router`. Everything else has a default implementation that can be customized by overriding methods, such as in the case of `httpErrorHandler` above. The server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

> **Note:** Play requires an [[application secret|ApplicationSecret]] to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.http.secret.key` system property.

Another way is to create a Play Application via [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) in combination with the `fromApplication` method:
 
@[application-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

## Logging configuration

When using Akka HTTP as an embedded server, no logging dependencies are included by default. If you want to also add logging to the embedded application, you can add the Play logback module:

@[embed-logging-sbt-dependencies](code/embedded.sbt)

And later call the [`LoggerConfigurator`](api/scala/play/api/LoggerConfigurator.html) API:

@[logger-akka-http](code/ScalaAkkaEmbeddingPlay.scala)