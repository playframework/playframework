<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Akka HTTP server backend _(experimental)_

> **Play experimental libraries are not ready for production use**. APIs may change. Features may not work properly.

Play 2's main server is built on top of [Netty](http://netty.io/). In Play 2.4 we started experimenting with an experimental server based on [Akka HTTP](http://doc.akka.io/docs/akka/2.4.3/scala/http/index.html). Akka HTTP is an HTTP library built on top of Akka. It is written by the authors of [Spray](http://spray.io/).

The purpose of this backend is:

* to check that the Akka HTTP API provides all the features that Play needs
* to gain knowledge about Akka HTTP in case we want to use it in Play in the future.

In future versions of Play we may implement a production quality Akka HTTP backend, but in Play 2.4 the Akka HTTP server is mostly a proof of concept. We do **not** recommend that you use it for anything other than learning about Play or Akka HTTP server code. In Play 2.4 you should always use the default Netty-based server for production code.

## Known issues

* Server shutdown is a bit rough. HTTP server actors are just killed.
* The implementation contains code duplicated from the Netty backend.
* Currently some Exception could not be handled by the HttpErrorHandler (Header Parsing Errors, Request Timeout).

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
    bind-timeout = 5 seconds
    
    # Enables/disables automatic handling of HEAD requests.
    # If this setting is enabled the server dispatches HEAD requests as GET
    # requests to the application and automatically strips off all message
    # bodies from outgoing responses.
    # Note that, even when this setting is off the server will never send
    # out message bodies on responses to HEAD requests.
    transparent-head-requests = on

    # How long a request takes until it times out
    # request-timeout = 240 seconds
    # If this setting is empty the server only accepts requests that carry a
    # non-empty `Host` header. Otherwise it responds with `400 Bad Request`.
    # Set to a non-empty value to be used in lieu of a missing or empty `Host`
    # header to make the server accept such requests.
    # Note that the server will never accept HTTP/1.1 request without a `Host`
    # header, i.e. this setting only affects HTTP/1.1 requests with an empty
    # `Host` header as well as HTTP/1.0 requests.
    # Examples: `www.spray.io` or `example.com:8080`
    default-host-header = ""

    # Enables/disables the addition of a `Remote-Address` header
    # holding the clients (remote) IP address.
    remote-address-header = off

    # The default value of the `Server` header to produce if no
    # explicit `Server`-header was included in a response.
    # If this value is the empty string and no header was included in
    # the request, no `Server` header will be rendered at all.
    server-header = ""
  }

}

akka {

  # Turn off dead letters until Akka HTTP server is stable
  log-dead-letters = off

}
```

> **Note:** In dev mode, when you use the `run` command, your `application.conf` settings will not be picked up by the server. This is because in dev mode the server starts before the application classpath is available. There are several [[other options|Configuration#Using-with-the-run-command]] you'll need to use instead.

## Embedded Usage

Play Akka HTTP server is also configurable as a embedded Play server. The simplest way to start an Play Akka HTTP Server is to use the [`AkkaHttpServer`](api/scala/play/core/server/akkahttp/AkkaHttpServer$.html) factory methods. If all you need to do is provide some straightforward routes, you may decide to use the [[String Interpolating Routing DSL|ScalaSirdRouter]] in combination with the `fromRouter` method:

@[simple-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

By default, this will start a server on port 9000 in prod mode.  You can configure the server by passing in a [`ServerConfig`](api/scala/play/core/server/ServerConfig.html):

@[config-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

You may want to customise some of the components that Play provides, for example, the HTTP error handler.  A simple way of doing this is by using Play's components traits, the [`AkkaServerComponents`](api/scala/play/core/server/akkahttp/AkkaServerComponents.html) trait is provided for this purpose, and can be conveniently combined with [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to build the application that it requires:

@[components-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

In this case, the server configuration can be overridden by overriding the `serverConfig` property.

To stop the server once you've started it, simply call the `stop` method:

@[stop-akka-http](code/ScalaAkkaEmbeddingPlay.scala)

> **Note:** Play requires an application secret to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.http.secret.key` system property.

Another way is to create a Play Application via [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) in combination with the `fromApplication` method:
 
@[application-akka-http](code/ScalaAkkaEmbeddingPlay.scala)
