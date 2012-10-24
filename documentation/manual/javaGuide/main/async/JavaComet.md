# Comet sockets

## Using chunked responses to create Comet sockets

An useful usage of **Chunked responses** is to create Comet sockets. A Comet socket is just a chunked `text/html` response containing only `<script>` elements. For each chunk, we write a `<script>` tag containing JavaScript that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and write it to the chunked response.
    
Let’s write a first proof-of-concept: create an enumerator generating `<script>` tags calling the browser `console.log` function:
    
```
public static Result index() {
  // Prepare a chunked text stream
  Chunks<String> chunks = new StringChunks() {

    // Called when the stream is ready
    public void onReady(Chunks.Out<String> out) {
      out.write("<script>console.log('kiki')</script>");
      out.write("<script>console.log('foo')</script>");
      out.write("<script>console.log('bar')</script>");
      out.close();
    }

  };

  response().setContentType("text/html");
  return ok(chunks);
}
```

If you run this action from a web browser, you will see the three events logged in the browser console.

## Using the `play.libs.Comet` helper

We provide a Comet helper to handle these comet chunked streams that does almost the same as what we just wrote.

> **Note:** Actually it does more, such as pushing an initial blank buffer data for browser compatibility, and supporting both String and JSON messages.

Let’s just rewrite the previous example to use it:

```
public static Result index() {
  Comet comet = new Comet("console.log") {
    public void onConnected() {
      sendMessage("kiki");
      sendMessage("foo");
      sendMessage("bar");
      close();
    }
  };
  
  ok(comet);
}
```

## The forever iframe technique

The standard technique to write a Comet socket is to load an infinite chunked comet response in an iframe and to specify a callback calling the parent frame:

```
public static Result index() {
  Comet comet = new Comet("parent.cometMessage") {
    public void onConnected() {
      sendMessage("kiki");
      sendMessage("foo");
      sendMessage("bar");
      close();
    }
  };
  
  ok(comet);
}
```

With an HTML page like:

```
<script type="text/javascript">
  var cometMessage = function(event) {
    console.log('Received event: ' + event)
  }
</script>

<iframe src="/comet"></iframe>
```

> **Next:** [[WebSockets | JavaWebSockets]]