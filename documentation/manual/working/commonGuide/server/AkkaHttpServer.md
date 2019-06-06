<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Akka HTTP Server Backend

Play uses the [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html) server backend to implement HTTP requests and responses using Akka Streams over the network.  Akka HTTP implements a full server stack for HTTP, including full HTTPS support, and has support for HTTP/2.

The Akka HTTP server backend is the default in Play. You can also use the [[Netty backend|NettyServer]] if you choose.

## Akka HTTP Implementation

Play's server backend uses the [low level server API](https://doc.akka.io/docs/akka-http/current/server-side/low-level-api.html?language=scala) to handle Akka's `HttpRequest` and `HttpResponse` classes.

Play's server backend automatically converts of an Akka `HttpRequest` into a Play HTTP request, so that details of the implementation are under the hood.  Play handles all the routing and application logic surrounding the server backend, while still providing the power and reliability of Akka-HTTP for processing requests.

## Working with Blocking APIs

Like the rest of Play, Akka HTTP is non-blocking.  This means that it uses a small number of threads which it keeps loaded with work at all times.

This poses a problem when working with blocking APIs such as JDBC or HTTPURLConnection, which cause a thread to wait until data has been returned from a remote system.

Please configure any work with blocking APIs off the main rendering thread, using a `Future` or `CompletionStage` configured with a `CustomExecutionContext` and using a custom thread pool defined in [[ThreadPools]].  See [[JavaAsync]] and [[ScalaAsync]] for more details.

## Configuring Akka HTTP

There are a variety of options that can be configured for the Akka HTTP server. These are given in the [[documentation on configuring Akka HTTP|SettingsAkkaHttp]].

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

If you need to use HTTP/2 in dev mode, you need to add a `-javaagent` argument for the Jetty ALPN agent to the Java options used to execute sbt

```
export SBT_OPTS="$SBT_OPTS -javaagent:$AGENT"
```

where `$AGENT` is the path to your Java agent. If you've already run `sbt stage`, you can find the path to the agent in your `target` directory:

```
export AGENT=$(pwd)/$(find target -name 'jetty-alpn-agent-*.jar' | head -1)
```

You also may want to write a simple script to run your app with the needed options, as demonstrated the `./play` script in the [play-scala-tls-example](https://github.com/playframework/play-scala-tls-example/blob/2.5.x/play)

## Manually selecting the Akka HTTP server

If for some reason you have both the Akka HTTP and the Netty server JARs on your classpath, then Play won't be able to predictably choose a server backend. You'll need to manually select the Akka HTTP server. This can be done by explicitly overriding the `play.server.provider` configuration option and setting it to a value of `play.core.server.AkkaHttpServerProvider`.

The `play.server.provider` configuration setting can be set in the same way as other configuration options. Different methods of setting configuration are described in the [[configuration file documentation|ConfigFile]]. Several examples of enabling the Akka HTTP server backend are shown below.

The recommended way to do this is to add the setting to two places. First, to enable Akka HTTP for the sbt `run` task, add the following to your `build.sbt`:

```
PlayKeys.devSettings += "play.server.provider" -> "play.core.server.AkkaHttpServerProvider"
```

Second, to enable the Akka HTTP backend for when you deploy your application or when you use the sbt `start` task, add the following to your `application.conf` file:

```
play.server.provider = play.core.server.AkkaHttpServerProvider
```

By adding the setting to both `build.sbt` and `application.conf` you can ensure that the Akka HTTP backend will be used in all cases.