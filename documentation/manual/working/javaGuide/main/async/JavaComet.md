<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Comet sockets

## Using chunked responses to create Comet sockets

An useful usage of **Chunked responses** is to create Comet sockets. A Comet socket is just a chunked `text/html` response containing only `<script>` elements. For each chunk, we write a `<script>` tag containing JavaScript that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and write it to the chunked response.
    
Let’s write a first proof-of-concept: create an enumerator generating `<script>` tags calling the browser `console.log` function:

@[manual](code/javaguide/async/JavaComet.java)

If you run this action from a web browser, you will see the three events logged in the browser console.

## Using the `play.libs.Comet` helper

We provide a Comet helper to handle these comet chunked streams that does almost the same as what we just wrote.

> **Note:** Actually it does more, such as pushing an initial blank buffer data for browser compatibility, and supporting both String and JSON messages.

Let’s just rewrite the previous example to use it:

@[comet](code/javaguide/async/JavaComet.java)

## The forever iframe technique

The standard technique to write a Comet socket is to load an infinite chunked comet response in an iframe and to specify a callback calling the parent frame:

@[forever-iframe](code/javaguide/async/JavaComet.java)

With an HTML page like:

```
<script type="text/javascript">
  var cometMessage = function(event) {
    console.log('Received event: ' + event)
  }
</script>

<iframe src="/comet"></iframe>
```
