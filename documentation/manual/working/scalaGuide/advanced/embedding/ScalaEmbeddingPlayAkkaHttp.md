<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
#  Embedding an Akka Http server in your application

Play Akka HTTP server is also configurable as a embedded Play server. The simplest way to start an Play Akka HTTP Server is to use the [`AkkaHttpServer`](api/scala/play/core/server/AkkaHttpServer$.html) factory methods. If all you need to do is provide some straightforward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouterWithComponents` method:

@[simple-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/play/core/server/ServerConfig.html):

@[config-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

You may want to customise some of the components that Play provides, for example, the HTTP error handler.  A simple way of doing this is by using Play's components traits, the [`AkkaHttpServerComponents`](api/scala/play/core/server/AkkaHttpServerComponents.html) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to build the application that it requires:

@[components-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

In this case, the server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

> **Note:** Play requires an application secret to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.http.secret.key` system property.

Another way is to create a Play Application via [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) in combination with the `fromApplication` method:
 
@[application-akka-http](code/ScalaAkkaEmbeddingPlay.scala)
