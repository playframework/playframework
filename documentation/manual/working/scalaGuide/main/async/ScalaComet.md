<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Comet

## Using chunked responses with Comet

A common use of **chunked responses** is to create a Comet socket.

A Comet socket is a chunked `text/html` response containing only `<script>` elements. For each chunk, we write a `<script>` tag containing JavaScript that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and write it to the chunked response.

Because `Ok.chunked` leverages [Akka Streams](http://doc.akka.io/docs/akka/2.4.2/scala/stream/index.html) to take a `Flow[ByteString]`, we can send a `Flow` of elements and transform it so that each element is escaped and wrapped in the Javascript method. The Comet helper automates Comet sockets, pushing an initial blank buffer data for browser compatibility, and supporting both String and JSON messages.  

## Comet Imports

To use the Comet helper, import the following classes:

@[comet-imports](code/ScalaComet.scala)

You will also need a materializer, which is best done by pulling `akka.stream.Materializer` from your [[DI system|ScalaDependencyInjection]].   

## Using Comet with String Flow

To push string messages through a Flow, do the following:

@[comet-string](code/ScalaComet.scala)

## Using Comet with JSON Flow

To push JSON messages through a Flow, do the following:

@[comet-json](code/ScalaComet.scala)

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

For an example of a Comet helper, see the [Play 2.5 Clock Template](https://github.com/typesafehub/play-2.5-clock/).

## Debugging Comet

The easiest way to debug a Comet stream that is not working is to use the [`log()`](http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0.3/scala/stream-cookbook.html#Logging_elements_of_a_stream) operation to show any errors involved in mapping data through the stream.

## Legacy Comet with Enumerator

Previously existing Comet functionality is still available through an `Enumeratee` using `Comet.apply`, but it is deprecated and you are encouraged to move to the Akka Streams based version.  

If you have existing code that relies heavily on `Enumerator`, you can use [`play.api.libs.streams.Streams`](api/scala/play/api/libs/streams/Streams$.html) and the interoperability with [Reactive Streams](http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0.3/scala/stream-integrations.html#integrating-with-reactive-streams) to convert an Enumerator to a Stream:

@[comet-enumerator](code/ScalaComet.scala)

Also see the [[Streams Migration Guide|StreamsMigration25#Migrating-Enumerators-to-Sources]].
