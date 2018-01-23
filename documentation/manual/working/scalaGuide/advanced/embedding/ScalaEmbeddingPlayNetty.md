<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Embedding a Netty server in your application

While Play apps are most commonly used as their own container, you can also embed a Play server into your own existing application. This can be used in conjunction with the Twirl template compiler and Play routes compiler, but these are of course not necessary. A common use case is an application with only a few simple routes.

One way to start a Play Netty Server is to use the [`NettyServer`](api/scala/play/core/server/NettyServer$.html) factory methods. If all you need to do is provide some straightforward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouterWithComponents` method:

@[simple](code/ScalaNettyEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/play/core/server/ServerConfig.html):

@[config](code/ScalaNettyEmbeddingPlay.scala)

Play also provides components traits that make it easy to customize other components besides the router. The [`NettyServerComponents`](api/scala/play/core/server/NettyServerComponents.html) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to build the application that it requires. In this example we use [`DefaultNettyServerComponents`](api/scala/play/core/server/DefaultNettyServerComponents.html), which is equivalent to `NettyServerComponents with BuiltInComponents with NoHttpFiltersComponents`:

@[components](code/ScalaNettyEmbeddingPlay.scala)

Here the only method you need to implement is `router`. Everything else has a default implementation that can be customized by overriding methods, such as in the case of `httpErrorHandler` above. The server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop](code/ScalaNettyEmbeddingPlay.scala)

> **Note:** Play requires an application secret to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.http.secret.key` system property.
