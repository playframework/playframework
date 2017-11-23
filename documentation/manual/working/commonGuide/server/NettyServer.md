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

When the Netty server is running it will tag all requests with a tag called `HTTP_SERVER` with a value of `netty`. The Akka HTTP backend will not have a value for this tag.

```scala
Action { request =>
  assert(request.tags.get("HTTP_SERVER") == "netty")
  ...
}
```

## Configuring Netty

See the [[SettingsNetty]] page.
