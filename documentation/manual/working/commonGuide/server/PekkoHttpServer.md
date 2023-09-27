<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Pekko HTTP Server Backend

Play uses the [Pekko HTTP](https://pekko.apache.org/docs/pekko-http/1.0/) server backend to implement HTTP requests and responses using Pekko Streams over the network.  Pekko HTTP implements a full server stack for HTTP, including full HTTPS support, and has support for HTTP/2.

The Pekko HTTP server backend is the default in Play. You can also use the [[Netty backend|NettyServer]] if you choose.

## Pekko HTTP Implementation

Play's server backend uses the [low level server API](https://pekko.apache.org/docs/pekko-http/1.0/server-side/low-level-api.html?language=scala) to handle Pekko's `HttpRequest` and `HttpResponse` classes.

Play's server backend automatically converts of an Pekko `HttpRequest` into a Play HTTP request, so that details of the implementation are under the hood.  Play handles all the routing and application logic surrounding the server backend, while still providing the power and reliability of Pekko-HTTP for processing requests.

## Working with Blocking APIs

Like the rest of Play, Pekko HTTP is non-blocking.  This means that it uses a small number of threads which it keeps loaded with work at all times.

This poses a problem when working with blocking APIs such as JDBC or HTTPURLConnection, which cause a thread to wait until data has been returned from a remote system.

Please configure any work with blocking APIs off the main rendering thread, using a `Future` or `CompletionStage` configured with a `CustomExecutionContext` and using a custom thread pool defined in [[ThreadPools]].  See [[JavaAsync]] and [[ScalaAsync]] for more details.

## Configuring Pekko HTTP

There are a variety of options that can be configured for the Pekko HTTP server. These are given in the [[documentation on configuring Pekko HTTP|SettingsPekkoHttp]].

## HTTP/2 support (incubating)

Play's Pekko HTTP server also supports HTTP/2. This feature is labeled "incubating" because the API may change in the future, and it has not been thoroughly tested in the wild. However, if you'd like to help Play improve please do test out HTTP/2 support and give us feedback about your experience.

You also should [[Configure HTTPS|ConfiguringHttps]] on your server before enabling HTTP/2. In general, browsers require TLS to work with HTTP/2, and Play's Pekko HTTP server uses ALPN (a TLS extension) to negotiate the protocol with clients that support it.

To add support for HTTP/2, add the `PlayPekkoHttp2Support` plugin. You can do this in an `enablePlugins` call for your project in `build.sbt`, for example:

@[enable-http2](code/pekko.http.server.sbt)

Adding the plugin will add the `play-pekko-http2-support` module, which provides the additional configuration for HTTP/2. By default, HTTP/2 is enabled, but it can be disabled by passing the `http2.enabled` system property, e.g. `play "start -Dhttp2.enabled=no"`.

You may want to write a simple script to run your app with the needed options, as demonstrated in the `./play` script in the [play-scala-tls-example](https://github.com/playframework/play-samples/tree/2.9.x/play-scala-tls-example) project.

> **Tip:** Use [nghttp2](https://nghttp2.org/documentation/nghttp.1.html) to run HTTP/2 requests against your application.

## Manually selecting the Pekko HTTP server

If for some reason you have both the Pekko HTTP and the Netty server JARs on your classpath, then Play won't be able to predictably choose a server backend. You'll need to manually select the Pekko HTTP server. This can be done by explicitly overriding the `play.server.provider` configuration option and setting it to a value of `play.core.server.PekkoHttpServerProvider`.

The `play.server.provider` configuration setting can be set in the same way as other configuration options. Different methods of setting configuration are described in the [[configuration file documentation|ConfigFile]]. Several examples of enabling the Pekko HTTP server backend are shown below.

The recommended way to do this is to add the setting to two places. First, to enable Pekko HTTP for the sbt `run` task, add the following to your `build.sbt`:

@[manually-select-pekko-http](code/pekko.http.server.sbt)

Second, to enable the Pekko HTTP backend for when you deploy your application or when you use the sbt `start` task, add the following to your `application.conf` file:

```
play.server.provider = play.core.server.PekkoHttpServerProvider
```

By adding the setting to both `build.sbt` and `application.conf` you can ensure that the Pekko HTTP backend will be used in all cases.
