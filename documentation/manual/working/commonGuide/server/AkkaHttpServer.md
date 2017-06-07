<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Akka HTTP Server Backend

Play uses the [Akka HTTP](http://doc.akka.io/docs/akka-http/current/) server backend to implement HTTP requests and responses using Akka Streams over the network.  Akka HTTP implements a full server stack for HTTP, including full HTTPS support, and has support for HTTP/2.

## Akka HTTP Implementation

Play's server backend uses the [low level server API](http://doc.akka.io/docs/akka-http/current/scala/http/low-level-server-side-api.html) to handle Akka's `HttpRequest` and `HttpResponse` classes.

Play's server backend automatically converts of an Akka `HttpRequest` into a Play HTTP request, so that details of the implementation are under the hood.  Play handles all the routing and application logic surrounding the server backend, while still providing the power and reliability of Akka-HTTP for processing requests.

## Working with Blocking APIs

Like the rest of Play, Akka HTTP is non-blocking.  This means that it uses a small number of threads which it keeps loaded with work at all times.

This poses a problem when working with blocking APIs such as JDBC or HTTPURLConnection, which cause a thread to wait until data has been returned from a remote system.

Please configure any work with blocking APIs off the main rendering thread, using a `Future` or `CompletionStage` configured with a `CustomExecutionContext` and using a custom thread pool defined in [[ThreadPools]].  See [[JavaAsync]] and [[ScalaAsync]] for more details.

## Configuring Akka HTTP

You can configure the Akka HTTP server settings through [[application.conf|SettingsAkkaHttp]]. That also describes how to enable the HTTP/2 support.

## HTTP/2 support (experimental)

Play's Akka HTTP server also supports HTTP/2. This feature is labeled "experimental" because the API may change in the future, and it has not been thoroughly tested in the wild. However, if you'd like to help Play improve please do test out HTTP/2 support and give us feedback about your experience.

You also should [[Configure HTTPS|ConfiguringHttps]] on your server before enabling HTTP/2. In general, browsers require TLS to work with HTTP/2, and Play's Akka HTTP server uses ALPN (a TLS extension) to negotiate the protocol with clients that support it.

To add support for HTTP/2, add the `PlayAkkaHttp2Support` plugin. You can do this in an `enablePlugins` call for your project in `build.sbt`, for example:

```
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PlayAkkaHttp2Support)
```

Adding the plugin will do multiple things:

 - It will add the `play-akka-http2-support` module, which provides extra configuration for HTTP/2 and depends on the `akka-http2-support` module. By default HTTP/2 is enabled. It can be disabled by passing the `http2.enabled` system property, e.g. `play "start -Dhttp2.enabled=no"`.
 - Configures the [Jetty ALPN agent](https://github.com/jetty-project/jetty-alpn-agent) as a Java agent using [sbt-javaagent](https://github.com/sbt/sbt-javaagent), and automatically adds the `-javaagent` argument for `start`, `stage` and `dist` tasks (i.e. production mode). This adds ALPN support to the JDK, allowing Akka HTTP to negotiate the protocol with the client. It *does not* configure for run mode. In JDK 9 this will not be an issue, since ALPN support is provided by default.

### Using HTTP/2 in `run` mode

If you need to use HTTP/2 in dev mode, you need to add a `-javaagent` argument for the Jetty ALPN agent to the Java options used to execute SBT

```
export SBT_OPTS="$SBT_OPTS -javaagent:$AGENT"
```

where `$AGENT` is the path to your Java agent. If you've already run `sbt stage`, you can find the path to the agent in your `target` directory:

```
export AGENT=$(pwd)/$(find target -name 'jetty-alpn-agent-*.jar' | head -1)
```

You also may want to write a simple script to run your app with the needed options, as demonstrated the `./play` script in the [play-scala-tls-example](https://github.com/playframework/play-scala-tls-example/blob/2.5.x/play)
