# Comet sockets

## Using chunked responses to create Comet sockets

A good use for **Chunked responses** is to create Comet sockets. A Comet socket is just a chunked `text/html` response containing only `<script>` elements. At each chunk we write a `<script>` tag that is immediately executed by the web browser. This way we can send events live to the web browser from the server: for each message, wrap it into a `<script>` tag that calls a JavaScript callback function, and writes it to the chunked response.
    
Let’s write a first proof-of-concept: an enumerator that generates `<script>` tags that each call the browser `console.log` JavaScript function:
    
```scala
def comet = Action {
  val events = Enumerator(
     """<script>console.log('kiki')</script>""",
     """<script>console.log('foo')</script>""",
     """<script>console.log('bar')</script>"""
  )
  Ok.stream(events >>> Enumerator.eof).as(HTML)
}
```

If you run this action from a web browser, you will see the three events logged in the browser console.

> **Tip:** Writing `events >>> Enumerator.eof` is just another way of writing `events.andThen(Enumerator.eof)`

We can write this in a better way by using `play.api.libs.iteratee.Enumeratee` that is just an adapter to transform an `Enumerator[A]` into another `Enumerator[B]`. Let’s use it to wrap standard messages into the `<script>` tags:
    
```scala
import play.api.templates.Html

// Transform a String message into an Html script tag
val toCometMessage = Enumeratee.map[String] { data => 
    Html("""<script>console.log('""" + data + """')</script>""")
}

def comet = Action {
  val events = Enumerator("kiki", "foo", "bar")
  Ok.stream(events >>> Enumerator.eof &> toCometMessage)
}
```

> **Tip:** Writing `events >>> Enumerator.eof &> toCometMessage` is just another way of writing `events.andThen(Enumerator.eof).through(toCometMessage)`

## Using the `play.api.libs.Comet` helper

We provide a Comet helper to handle these Comet chunked streams that do almost the same stuff that we just wrote.

> **Note:** Actually it does more, like pushing an initial blank buffer data for browser compatibility, and it supports both String and JSON messages. It can also be extended via type classes to support more message types.

Let’s just rewrite the previous example to use it:

```scala
def comet = Action {
  val events = Enumerator("kiki", "foo", "bar")
  Ok.stream(events &> Comet(callback = "console.log"))
}
```

## The forever iframe technique

The standard technique to write a Comet socket is to load an infinite chunked comet response in an HTML `iframe` and to specify a callback calling the parent frame:

```scala
def comet = Action {
  val events = Enumerator("kiki", "foo", "bar")
  Ok.stream(events &> Comet(callback = "parent.cometMessage"))
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

> **Next:** [[WebSockets | ScalaWebSockets]]