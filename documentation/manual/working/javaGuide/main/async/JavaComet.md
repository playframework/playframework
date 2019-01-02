<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Comet

## Using chunked responses with Comet

A common use of **chunked responses** is to create Comet sockets.

A Comet socket is a chunked `text/html` response containing only `<script>` elements. For each chunk, we write a `<script>` tag containing JavaScript that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and write it to the chunked response.

Because `ok().chunked` leverages [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=java) to take a `Flow<ByteString>`, we can send a `Flow` of elements and transform it so that each element is escaped and wrapped in the Javascript method. The Comet helper automates Comet sockets, pushing an initial blank buffer data for browser compatibility, and supporting both String and JSON messages.

## Comet Imports

To use the Comet helper, import the following classes:

@[comet-imports](code/javaguide/async/JavaComet.java)

You will also need a materializer, which is best done by pulling `akka.stream.Materializer` from your [[DI system|JavaDependencyInjection]].    

## Using Comet with String Flow

To push string messages through a Flow, do the following:

@[comet-string](code/javaguide/async/JavaComet.java)

## Using Comet with JSON Flow

To push JSON messages through a Flow, do the following:

@[comet-json](code/javaguide/async/JavaComet.java)

## Using Comet with iframe

The comet helper should typically be used with a `forever-iframe` technique, with an HTML page like:

```
<script type="text/javascript">
  var cometMessage = function(event) {
    console.log('Received event: ' + event)
  }
</script>

<iframe src="/comet"></iframe>
```

> **Note**: add the following config to your application.conf and also ensure that you have route mappings set up in order to see the above comet in action.
```
play.filters.headers {
  frameOptions = "SAMEORIGIN"
  contentSecurityPolicy = "connect-src 'self'"
}
```

For an example of a Comet helper, see the [Play 2.5 Clock Template](https://github.com/typesafehub/play-2.5-clock/).

## Debugging Comet

The easiest way to debug a Comet stream that is not working is to use the [`log()`](https://doc.akka.io/docs/akka/2.5/stream/stream-cookbook.html?language=java#logging-in-streams) operation to show any errors involved in mapping data through the stream.

## Legacy Comet Functionality

Previously existing Comet functionality is still available through `play.libs.Comet`, but it is deprecated and you are encouraged to move to the Akka Streams based version.

Because the Java Comet helper is based around callbacks, it may be easier to turn the callback based functionality into a `org.reactivestreams.Publisher` directly and use `Source.fromPublisher` to create a source.

