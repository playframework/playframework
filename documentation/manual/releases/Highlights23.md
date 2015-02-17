<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.3

This page highlights the new features of Play 2.3. If you want learn about the changes you need to make to migrate to Play 2.3, check out the [[Play 2.3 Migration Guide|Migration23]].

## Activator

The first thing you'll notice about Play 2.3 is that the `play` command has become the `activator` command. Play has been updated to use [Activator](https://typesafe.com/activator) so that we can:

* Extend the range of templates we provide for getting started with Play projects. Activator supports a much [richer library](https://typesafe.com/activator/templates) of project templates. Templates can also include tutorials and other resources for getting started. The Play community can [contribute templates](https://typesafe.com/activator/template/contribute) too.
* Provide a nice web UI for getting started with Play, especially for newcomers who are unfamiliar with command line interfaces. Users can write code and run tests through the web UI. For experienced users, the command line interface is available just like before.
* Make Play's high productivity development approach available to other projects. Activator isn't just for Play. Other projects can use Activator too.

In the future Activator will get even more features, and these features will automatically benefit Play and other projects that use Activator. [Activator is open source](https://github.com/typesafehub/activator), so the community can contribute to its evolution.

### Activator command

All the features that were available with the `play` command are still available with the `activator` command.

* `activator new` to create a new project. See [[Creating a new application|NewApplication]].
* `activator` to run the console. See [[Using the Play console|PlayConsole]].
* `activator ui` is a new command that launches a web user interface.

> The new `activator` command and the old `play` command are both wrappers around [sbt](http://www.scala-sbt.org/). If you prefer, you can use the `sbt` command directly. However, if you use sbt you will miss out on several Activator features, such as templates (`activator new`) and the web user interface (`activator ui`). Both sbt and Activator support all the usual console commands such as `test` and `run`.

### Activator distribution

Play is distributed as an Activator distribution that contains all Play's dependencies. You can download this distribution from the [Play download](http://www.playframework.com/download) page.

If you prefer, you can also download a minimal (1MB) version of Activator from the [Activator site](https://typesafe.com/activator). Look for the "mini" distribution on the download page. The minimal version of Activator will only download dependencies when they're needed.

Since Activator is a wrapper around sbt, you can also download and use [sbt](http://www.scala-sbt.org/) directly, if you prefer.

## Build improvements

### sbt-web

The largest new feature for Play 2.3 is the introduction of [sbt-web](https://github.com/sbt/sbt-web#sbt-web). In summary sbt-web allows HTML, CSS and JavaScript functionality to be factored out of Play's core into a family of pure sbt plugins. There are two major advantages to you:

* Play is less opinionated on the HTML, CSS and JavaScript; and
* sbt-web can have its own community and thrive in parallel to Play's.

### Auto Plugins

Play now uses sbt 0.13.5. This version brings a new feature named "auto plugins" which, in essence permits a large reduction in settings-oriented code for your build files.

### Asset Pipeline and Fingerprinting

sbt-web brings the notion of a highly configurable asset pipeline to Play e.g.:

```scala
pipelineStages := Seq(rjs, digest, gzip)
```

The above will order the RequireJs optimizer (sbt-rjs), the digester (sbt-digest) and then compression (sbt-gzip). Unlike many sbt tasks, these tasks will execute in the order declared, one after the other.

One new capability for Play 2.3 is the support for asset fingerprinting, similar in principle to [Rails asset fingerprinting](http://guides.rubyonrails.org/asset_pipeline.html#what-is-fingerprinting-and-why-should-i-care-questionmark). A consequence of asset fingerprinting is that we now use far-future cache expiries when they are served. The net result of this is that your user's will experience faster downloads when they visit your site given the aggressive caching strategy that a browser is now able to employ.

### Default ivy cache and local repository

Play now uses the default ivy cache and repository, in the `.ivy2` folder in the users home directory.

This means Play will now integrate better with other sbt builds, not requiring artifacts to be cached multiple times, and allowing the sharing of locally published artifacts.

## Java improvements

### Java 8

Play 2.3 has been tested with Java 8. Your project will work just fine with Java 8; there is nothing special to do other than ensuring that your Java environment is configured for Java 8. There is a new Activator sample available for Java 8:

http://typesafe.com/activator/template/reactive-stocks-java8

Our documentation has been improved with Java examples in general and, where applicable, Java 8 examples. Check out some [[examples of asynchronous programming with Java 8|JavaAsync]].

For a complete overview of going Reactive with Java 8 and Play check out this blog: http://typesafe.com/blog/go-reactive-with-java-8

### Java performance

We've worked on Java performance. Compared to Play 2.2, throughput of simple Java actions has increased by 40-90%. Here are the main optimizations:

* Reducing thread switches for Java actions and body parsers.
* Caching more route information and using per-route caching rather than a shared Map.
* Reducing body parsing overhead for GET requests.
* Using a unicast enumerator for returning chunked responses.

Some of these changes also improved Scala performance, but Java had the biggest performance gains and was the main focus of our work.

Thankyou to [YourKit](http://yourkit.com) for supplying the Play team with licenses to make this work possible.

## Scala 2.11

Play 2.3 is the first release of Play to have been cross built against multiple versions of Scala, both 2.10 and 2.11.

You can select which version of Scala you would like to use by setting the `scalaVersion` setting in your `build.sbt` or `Build.scala` file.

For Scala 2.11:

```scala
scalaVersion := "2.11.1"
```

For Scala 2.10:

```scala
scalaVersion := "2.10.4"
```

## Play WS

### Separate library

The WS client library has been refactored into its own library which can be used outside of Play. You can now have multiple `WSClient` objects, rather than only using the `WS` singleton.

[[Java|JavaWS]]

```java
WSClient client = new NingWSClient(config);
Promise<WSResponse> response = client.url("http://example.com").get();
```

[[Scala|ScalaWS]]

```scala
val client: WSClient = new NingWSClient(config)
val response = client.url("http://example.com").get()
```

Each WS client can be configured with its own options. This allows different Web Services to have different settings for timeouts, redirects and security options.

The underlying `AsyncHttpClient` object can also now be accessed, which means that multi-part form and streaming body uploads are supported.

### WS Security

WS clients have [[settings|WsSSL]] for comprehensive SSL/TLS configuration. WS client configuration is now more secure by default.

## Actor WebSockets

A method to use actors for handling websocket interactions has been incorporated for both Java and Scala e.g. using Scala:

[[Java|JavaWebSockets]]

```java
public static WebSocket<String> socket() {
    return WebSocket.withActor(MyWebSocketActor::props);
}
```

[[Scala|ScalaWebSockets]]

```scala
def webSocket = WebSocket.acceptWithActor[JsValue, JsValue] { req => out =>
  MyWebSocketActor.props(out)
```

## Results restructuring completed

In Play 2.2, a number of new result types were introduced and old results types deprecated. Play 2.3 finishes this restructuring. See *Results restructure* in the [[Migration Guide|Migration23]] for more information.

## Anorm

There are various fixes included in Play 2.3's Anorm (type safety, option parsing, error handling, ...) and new interesting features.

- String interpolation is available to write SQL statements more easily, with less verbosity (passing arguments) and performance improvements (up to x7 faster processing parameters). e.g. `SQL"SELECT * FROM table WHERE id = $id"`
- Multi-value (sequence/list) can be passed as parameter. e.g. `SQL"""SELECT * FROM Test WHERE cat IN (${Seq("a", "b", "c")})"""`
- It's now possible to parse column by position. e.g. `val parser = long(1) ~ str(2) map { case l ~ s => ??? }`
- Query results include not only data, but execution context (with SQL warning).
- More types are supported as parameter and as column: `java.util.UUID`, numeric types (Java/Scala big decimal and integer, more column conversions between numerics), temporal types (`java.sql.Timestamp`), character types.

## Custom SSLEngine for HTTPS

The Play server can now [[use a custom `SSLEngine`|ConfiguringHttps]]. This is also useful in cases where customization is required, such as in the case of client authentication.
