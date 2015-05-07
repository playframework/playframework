<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Akka HTTP server backend _(experimental)_

> **Play experimental libraries are not ready for production use**. APIs may change. Features may not work properly.

Play 2's main server is built on top of [Netty](http://netty.io/). In Play 2.4 we started experimenting with an experimental server based on [Akka HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/current/). Akka HTTP is an HTTP library built on to of Akka. It is written by the authors of [Spray](http://spray.io/).

The purpose of this backend is:

* to check that the Akka HTTP API provides all the features that Play needs
* to gain knowledge about Akka HTTP in case we want to use it in Play in the future.

In future versions of Play we may implement a production quality Akka HTTP backend, but in Play 2.4 the Akka HTTP server is mostly a proof of concept. We do **not** recommend that you use it for anything other than learning about Play or Akka HTTP server code. In Play 2.4 you should always use the default Netty-based server for production code.

## Known issues

* Slow. There is a lot more copying in the Akka HTTP backend because the Play and Akka HTTP APIs are not naturally compatible. A lot of extra copying is needed to translate the objects.
* WebSockets are not supported, due to missing support in Akka HTTP.
* No HTTPS support, again due to missing support in Akka HTTP.
* Server shutdown is a bit rough. HTTP server actors are just killed.
* The implementation contains code duplicated from the Netty backend.

## Usage

To use the Akka HTTP server backend you first need to disable the Netty server and add the Akka HTTP server plugin to your project:

```scala
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PlayAkkaHttpServer)
  .disablePlugins(PlayNettyServer)
```

Now Play should automatically select the Akka HTTP server for running in dev mode, prod and in tests.

### Manually selecting the Akka HTTP server

If for some reason you have both the Akka HTTP server and the Netty HTTP server on your classpath, you'll need to manually select it.  This can be done using the `play.server.provider` system property, for example, in dev mode:

```
run -Dplay.server.provider=play.core.server.akkahttp.AkkaHttpServerProvider
```

### Verifying that the Akka HTTP server is running

When the Akka HTTP server is running it will tag all requests with a tag called `HTTP_SERVER` with a value of `akka-http`. The Netty backend will not have a value for this tag.

```scala
Action { request =>
  assert(request.tags.get("HTTP_SERVER") == Some("akka-http"))
  ...
}
```

### Configuring the Akka HTTP server

The Akka HTTP server is configured with Typesafe Config, like the rest of Play. This is the default configuration for the Akka HTTP backend. The `log-dead-letters` setting is set to `off` because the Akka HTTP server can send a lot of letters. If you want this on then you'll need to enable it in your `application.conf`.

```
play {

  # The server provider class name
  server.provider = "play.core.server.akkahttp.AkkaHttpServerProvider"

  akka {
    # How long to wait when binding to the listening socket
    http-bind-timeout = 5 seconds
  }

}

akka {

  # Turn off dead letters until Akka HTTP server is stable
  log-dead-letters = off

}
```

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|Configuration#Using-with-the-run-command]] you'll need to use instead.