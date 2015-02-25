<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Akka HTTP server backend (experimental)

> **Play experimental libraries are not ready for production use**. APIs may change. Features may not work properly.

The Play 2 APIs are built on top of an HTTP server backend. The default HTTP server backend uses the [Netty](http://netty.io/) library. In Play 2.4 another **experimental** backend, based on [Akka HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/current/), is also available. The Akka HTTP backend aims to provide the same Play API as the Netty HTTP backend. At the moment the Akka HTTP backend is missing quite a few features.

The experimental Akka HTTP backend is a technical proof of concept. It is not intended for production use and it doesn't implement the full Play API. The purpose of the new backend is to trial Akka HTTP as a possible backend for a future version of Play. The backend also serves as a valuable test case for our friends on the Akka project.

## Known issues

* WebSockets are not supported. This will be fixed once Akka HTTP gains WebSocket support.
* No HTTPS support.
* If a `Content-Length` header is not supplied, the Akka HTTP server always uses chunked encoding. This is different from the Netty backend which will automatically buffer some requests to get a `Content-Length`.
* No `X-Forwarded-For` support.
* No `RequestHeader.username` support.
* Server shutdown is a bit rough now. HTTP server actors are just killed.
* No attempt has been made to tune performance. Performance will to be slower than Netty. For example, currently there is a lot of extra copying between Play's `Array[Byte]` and Akka's `ByteString`. This could be optimized.
* The implementation contains a lot of code duplicated from Netty.
* There are no proper documentation tests for the code written on this page.

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

The Akka HTTP server is configured with Typesafe Config, like the rest of Play. Note: when running Play in development mode, the current project's resources may not be available on the server's classpath. Configuration may need to be provided in system properties or via resources in a JAR file.

```
play {

  # Configuration for Play's AkkaHttpServer
  akka-http-server {

    # The name of the ActorSystem
    actor-system = "play-akka-http-server"

    # How long to wait when binding to the listening socket
    http-bind-timeout = 5 seconds

    akka {
      loggers = ["akka.event.Logging$DefaultLogger", "akka.event.slf4j.Slf4jLogger"]
      loglevel = WARNING

      # Turn off dead letters until server is stable
      log-dead-letters = off

      actor {
        default-dispatcher = {
          fork-join-executor {
            parallelism-factor = 1.0
            parallelism-max = 24
          }
        }

      }

    }

  }

}
```
