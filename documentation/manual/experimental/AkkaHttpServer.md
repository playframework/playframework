<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
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

To use the Akka HTTP server backend you first need to add the Akka HTTP server module as a dependency of your project.

```scala
libraryDependencies += "com.typesafe.play" %% "play-akka-http-server-experimental" % "%PLAY_VERSION%"
```

Next you need to tell Play to use the new server backend. You can do this with a system property or by using a different class to start your application.

### Dev mode using a system property

To run dev mode with the Akka HTTP server you need to supply a system property when you call the `run` command.

```
run -Dserver.provider=play.core.server.akkahttp.AkkaHttpServerProvider
```

### Functional testing using a system property

You can use the Akka HTTP server when using the `WithServer` class in your [[functional tests|ScalaFunctionalTestingWithSpecs2]]. Supplying a system property will change the server used by all your tests.

To run tests with a system property you need to change your sbt settings to fork the tests and then supply the system property as an argument to Java.

```scala
fork in Test := true

javaOptions in Test += "-Dserver.provider=play.core.server.akkahttp.AkkaHttpServerProvider"
```

### Functional testing using a ServerProvider class

Instead of using a system property you can supply a `ServerProvider` instance to the `WithServer` class in your [[functional tests|ScalaFunctionalTestingWithSpecs2]].

```scala
import play.core.server.akkahttp.AkkaHttpServer

"use the Akka HTTP server in a test" in new WithServer(
  app = myApp, serverProvider = AkkaHttpServer.defaultServerProvider) {
  val response = await(WS.url("http://localhost:19001/testpath").get())
  response.status must equalTo(OK)
}
```

### Deployed app with a system property

Once you've deployed your app with `dist` you can tell it to use the Akka HTTP server by [[providing a system property|ProductionConfiguration]] when you run it.

```
/path/to/bin/<project-name> -Dserver.provider=play.core.server.akkahttp.AkkaHttpServerProvider
```

### Deployed app with a different main class

Instead of using Netty, you can choose to use the Akka HTTP server's main class when you deploy your application. That means the application will always start with the Akka HTTP server backend.

Change the main class in your sbt settings.

```scala
mainClass in Compile := Some("play.core.server.akkahttp.AkkaHttpServer")
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