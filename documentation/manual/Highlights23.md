<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.3

## Distribution

Play no longer has the `play` command as it is entirely integrated with [Typesafe Activator](https://typesafe.com/activator). If you are particularly adverse to large downloads, you can get a minimal version of [Activator from that page](https://typesafe.com/activator).

Play has always provided an ultra productive development environment both suited well to newcomers and advanced users.  At Typesafe we wanted to take this developer experience further providing a library of community contributed templates, tutorials and resources for getting started, additional developer tools and an even more productive development environment.  But we didn't want to just limit it to Play and so evolved it to something that was bigger and better.  Activator is the next evolution of the Play command.

## Build tasks

### Auto Plugins

sbt 0.13.5 is now the version used by Play. This version brings a new feature named "auto plugins" which, in essence permit a large reduction in settings oriented code for your build files.

### sbt-web

The largest new feature for Play 2.3 is the introduction of [sbt-web](https://github.com/sbt/sbt-web#sbt-web). In summary sbt-web allows Html, CSS and JavaScript functionality to be factored out of Play's core into a family of pure sbt plugins. There are two major advantages to you:

* Play is less opinionated on the Html, CSS and JavaScript; and
* sbt-web can have its own community and thrive in parallel to Play's.

## Results structure

In Play 2.2, a number of result types were deprecated, and to facilitate migration to the new results structure, some new types introduced.  Play 2.3 finishes this restructuring.

### Scala results

Deprecated types have been removed and `play.api.mvc.SimpleResult` has been renamed to `play.api.mvc.Result`, replacing the existing `Result` trait.

### Java results

Deprecated types have been removed and `play.mvc.SimpleResult` has been renamed to `play.mvc.Result`. 

## Play WS

The WS client has been factored out into its own library in order to further reduce the size of play.core and promote modularization.

## Actor WebSockets

A method to use actors for handling websocket interactions has been incorporated for both Java and Scala e.g. using Scala:

```scala
   def webSocket = WebSocket.acceptWithActor[JsValue, JsValue] { req => out =>
     MyWebSocketActor.props(out)
```

## Java 8

Play 2.3 will work just fine with Java 8; there is nothing special to do other than ensuring that your Java environment is configured for Java 8. There is a new Activator sample available for Java 8:

http://typesafe.com/activator/template/reactive-stocks-java8

Our documentation has been improved with Java examples in general and, where applicable, Java 8 examples.

For a complete overview of going Reactive with Java 8 and Play check out this blog: http://typesafe.com/blog/go-reactive-with-java-8
