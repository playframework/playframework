<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

#  Embedding an Pekko Http server in your application

While Play apps are most commonly used as their own container, you can also embed a Play server into your own existing application. This can be used in conjunction with the Twirl template compiler and Play routes compiler, but these are of course not necessary. A common use case is an application with only a few simple routes. To use Pekko HTTP Server embedded, you will need the following dependency:

@[pekko-http-sbt-dependencies](code/embedded.sbt)

One way to start a Play Pekko HTTP Server is to use the [`PekkoHttpServer`](api/scala/play/core/server/PekkoHttpServer$.html) factory methods. If all you need to do is provide some straightforward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouterWithComponents` method:

@[simple-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/play/core/server/ServerConfig.html):

@[config-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)

Play also provides components traits that make it easy to customize other components besides the router. The [`PekkoHttpServerComponents`](api/scala/play/core/server/PekkoHttpServerComponents.html) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to build the application that it requires. In this example we use [`DefaultPekkoHttpServerComponents`](api/scala/play/core/server/DefaultPekkoHttpServerComponents.html), which is equivalent to `PekkoHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents`:

@[components-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)

Here the only method you need to implement is `router`. Everything else has a default implementation that can be customized by overriding methods, such as in the case of `httpErrorHandler` above. The server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)

> **Note:** Play requires an [[application secret|ApplicationSecret]] to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.http.secret.key` system property.

Another way is to create a Play Application via [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) in combination with the `fromApplication` method:
 
@[application-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)

## Logging configuration

When using Pekko HTTP as an embedded server, no logging dependencies are included by default. If you want to also add logging to the embedded application, you can add the Play logback module:

@[embed-logging-sbt-dependencies](code/embedded.sbt)

And later call the [`LoggerConfigurator`](api/scala/play/api/LoggerConfigurator.html) API:

@[logger-pekko-http](code/ScalaPekkoEmbeddingPlay.scala)
