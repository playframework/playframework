<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
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

If for some reason you have both the Akka HTTP and the Netty server JARs on your classpath, then Play won't be able to predictably choose a server backend. You'll need to manually select the Netty server. This can be done by explicitly overriding the `play.server.provider` configuration option and setting it to a value of `play.core.server.NettyServerProvider`.

The `play.server.provider` configuration setting can be set in the same way as other configuration options. Different methods of setting configuration are described in the [[configuration file documentation|ConfigFile]]. Several examples of enabling the Netty server are shown below.

The recommended way to do this is to add the setting to two places. First, to enable Netty for the sbt `run` task, add the following to your `build.sbt`:

```
PlayKeys.devSettings += "play.server.provider" -> "play.core.server.NettyServerProvider"
```

Second, to enable the Netty backend for when you deploy your application or when you use the sbt `start` task, add the following to your `application.conf` file:

```
play.server.provider = play.core.server.NettyServerProvider
```

By adding the setting to both `build.sbt` and `application.conf` you can ensure that the Netty backend will be used in all cases.

## Verifying that the Netty server is running

When the Netty server is running the request attribute `RequestAttrKey.Server` with the value `netty` will be set for all requests. The Akka HTTP backend will not set a value for this request attribute.

Scala
: @[server-request-attribute](code/SomeScalaController.scala)

Java
: @[server-request-attribute](code/SomeJavaController.java)

## Configuring Netty

See the [[SettingsNetty]] page.
