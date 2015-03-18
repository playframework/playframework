<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Embedding a Play server in your application

While Play apps are most commonly used as their own container, you can also embed a Play server into your own existing application.  This can be used in conjunction with the Twirl template compiler and Play routes compiler, but these are of course not necessary, a common use case for embedding a Play application will be because you only have a few very simple routes.

The simplest way to start an embedded Play server is to use the [`NettyServer`](api/scala/index.html#play.core.server.NettyServer$) factory methods.  If all you need to do is provide some straight forward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouter` method:

@[simple](code/ScalaEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/index.html#play.core.server.ServerConfig):

@[config](code/ScalaEmbeddingPlay.scala)

You may want to customise some of the components that Play provides, for example, the HTTP error handler.  A simple way of doing this is by using Play's components traits, the [`NettyServerComponents`](api/scala/index.html#play.core.server.NettyServerComponents) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/index.html#play.api.BuiltInComponents) to build the application that it requires:

@[components](code/ScalaEmbeddingPlay.scala)

In this case, the server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop](code/ScalaEmbeddingPlay.scala)

> **Note:** Play requires an application secret to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.crypto.secret` system property.