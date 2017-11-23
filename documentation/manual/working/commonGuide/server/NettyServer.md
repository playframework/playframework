<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Netty Server Backend

Prior to Play 2.6.x, Play used the Netty server backend as the default.  In 2.6.x, the default backend was changed to Akka HTTP, but you can still manually select the Netty backend server in your project.

## Usage

To use the Netty server backend you first need to disable the Akka HTTP server and add the Netty server plugin to your project:

```scala
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PlayNettyServer)
  .disablePlugins(PlayAkkaHttpServer)
```

Now Play should automatically select the Netty server for running in dev mode, prod and in tests.

## Manually selecting the Netty server

If for some reason you have both the Akka HTTP server and the Netty HTTP server on your classpath, you'll need to manually select it.  This can be done using the `play.server.provider` system property, for example, in dev mode:

```
run -Dplay.server.provider=play.core.server.NettyServerProvider
```

## Verifying that the Netty server is running

When the Netty server is running the request attribute `RequestAttrKey.Server` with the value `netty` will be set for all requests. The Akka HTTP backend will not set a value for this request attribute.

Scala
: @[server-request-attribute](code/SomeScalaController.scala)

Java
: @[server-request-attribute](code/SomeJavaController.java)

## Configuring Netty

See the [[SettingsNetty]] page.
