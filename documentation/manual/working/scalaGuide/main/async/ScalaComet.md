<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Comet sockets

## Using chunked responses to create Comet sockets

A good use for **Chunked responses** is to create Comet sockets. A Comet socket is just a chunked `text/html` response containing only `<script>` elements. At each chunk we write a `<script>` tag that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and writes it to the chunked response.
    
Let’s write a first proof-of-concept: an enumerator that generates `<script>` tags that each call the browser `console.log` JavaScript function:
    
@[manual](code/ScalaComet.scala)

If you run this action from a web browser, you will see the three events logged in the browser console.

We can write this in a better way by using `play.api.libs.iteratee.Enumeratee` that is just an adapter to transform an `Enumerator[A]` into another `Enumerator[B]`. Let’s use it to wrap standard messages into the `<script>` tags:
    
@[enumeratee](code/ScalaComet.scala)

> **Tip:** Writing `events &> toCometMessage` is just another way of writing `events.through(toCometMessage)`

## Using the `play.api.libs.Comet` helper

We provide a Comet helper to handle these Comet chunked streams that do almost the same stuff that we just wrote.

> **Note:** Actually it does more, like pushing an initial blank buffer data for browser compatibility, and it supports both String and JSON messages. It can also be extended via type classes to support more message types.

Let’s just rewrite the previous example to use it:

@[helper](code/ScalaComet.scala)

## The forever iframe technique

The standard technique to write a Comet socket is to load an infinite chunked comet response in an HTML `iframe` and to specify a callback calling the parent frame:

@[iframe](code/ScalaComet.scala)

With an HTML page like:

```
<script type="text/javascript">
  var cometMessage = function(event) {
    console.log('Received event: ' + event)
  }
</script>

<iframe src="/comet"></iframe>
```
