<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.3

## Activator

The first thing you'll notice about Play 2.3 is that the `play` command has become the `activator` command. Play has moved to use [Typesafe Activator](https://typesafe.com/activator) so that we can:

* Extend the range of templates we provide for getting started with Play projects. Activator supports a much [richer library](https://typesafe.com/activator/templates) of project templates. Templates can also include tutorials and other resources for getting started. The Play community can [contribute templates](https://typesafe.com/activator/template/contribute) too.
* Provide a nice web UI for getting started with Play, especially for newcomers who are unfamiliar with command line interfaces. Users can write code and run tests through the web UI. The command line interface is still available though!
* Make Play's high productivity development approach available to other projects. Activator isn't just for Play. Other projects can use Activator too.
* In the future Activator is likely to get even more features to aid developer productivity, and these features will automatically benefit Play and other projects that use Activator. [Activator is open source](https://github.com/typesafehub/activator), so the community can contribute to its evolution.

### Activator command

All the features that were available with the `play` command are still available with the `activator` command.

* `activator new` to create a new project. See [[Creating a new application|NewApplication]].
* `activator` to run the console. See [[Using the Play console|PlayConsole]].
* `activator ui` is a new command that launches a web user interface.

### Activator distribution

Play is distributed as an Activator distribution that contains all Play's dependencies. You can download this distribution from the [Play download](http://www.playframework.com/download) page. If you prefer, you can also download a minimal (1MB) version of Activator from the [Activator site](https://typesafe.com/activator). The minimal version will only download dependencies when they're needed.

## Build improvements

### sbt-web

The largest new feature for Play 2.3 is the introduction of [sbt-web](https://github.com/sbt/sbt-web#sbt-web). In summary sbt-web allows HTML, CSS and JavaScript functionality to be factored out of Play's core into a family of pure sbt plugins. There are two major advantages to you:

* Play is less opinionated on the HTML, CSS and JavaScript; and
* sbt-web can have its own community and thrive in parallel to Play's.

### Auto Plugins

Play now uses sbt 0.13.5. This version brings a new feature named "auto plugins" which, in essence permits a large reduction in settings-oriented code for your build files.

## Java improvements

### Java 8

Play 2.3 will work just fine with Java 8; there is nothing special to do other than ensuring that your Java environment is configured for Java 8. There is a new Activator sample available for Java 8:

http://typesafe.com/activator/template/reactive-stocks-java8

Our documentation has been improved with Java examples in general and, where applicable, Java 8 examples. Check out some [[examples of asynchronous programming with Java 8|JavaAsync]].

For a complete overview of going Reactive with Java 8 and Play check out this blog: http://typesafe.com/blog/go-reactive-with-java-8

### Java performance

We've worked on Java performance. Compared to Play 2.2, throughput of simple Java actions has increased by 40-90%.

## Play WS

The WS client has been factored out into its own library in order to further reduce the size of play.core and promote modularization.

It is now possible to use WS outside of the context of an application:

```
val client:WSClient = new NingWSClient(new Builder().build())
client.url("http://example.com").get()
```

The WS client also comes with comprehensive SSL/TLS configuration options and configures JSSE to be secure by default.

## Actor WebSockets

A method to use actors for handling websocket interactions has been incorporated for both Java and Scala e.g. using Scala:

```scala
   def webSocket = WebSocket.acceptWithActor[JsValue, JsValue] { req => out =>
     MyWebSocketActor.props(out)
```

## Results restructuring completed

In Play 2.2, a number of new result types were introduced and old results types deprecated. Play 2.3 finishes this restructuring. See *Results restructure* in the [[Migration Guide|Migration23]] for more information.

## Anorm

There are various fixes included in new Anorm (type safety, option parsing, error handling, ...) and new interesting features.

- String interpolation is available to write SQL statements more easily, with less verbosity (passing arguments) and performance improvements (up to x7 faster processing parameters). e.g. `SQL"SELECT * FROM table WHERE id = $id"`
- Multi-value (sequence/list) can be passed as parameter. e.g. `SQL"""SELECT * FROM Test WHERE cat IN (${Seq("a", "b", "c")})"""`
- It's now possible to parse column by position. e.g. `val parser = long(1) ~ str(2) map { case l ~ s => ??? }`
- Query results include not only data, but execution context (with SQL warning).
- More types are supported as parameter and as column: `java.util.UUID`, numeric types (Java/Scala big decimal and integer, more column conversions between numerics), temporal types (`java.sql.Timestamp`), character types.

