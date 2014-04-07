<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.3

## Distribution

Play no longer has the `play` command as it is entirely integrated with [Typesafe Activator](https://typesafe.com/activator).

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