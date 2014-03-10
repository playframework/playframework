# WebSockets

## Using WebSockets instead of Comet sockets

A Comet socket is a kind of hack for sending live events to the web browser. Also, it only supports one-way communication from the server to the client. To push events to the server, the web browser has to send Ajax requests.

> **Note:** It is also possible to achieve the same kind of live communication the other way around by using an infinite HTTP request handled by a custom `BodyParser` that receives chunks of input data, but that is far more complicated.

Modern web browsers natively support two-way live communication via WebSockets.

>WebSocket is a web technology that provides bi-directional, full-duplex communication channels, over a single Transmission Control Protocol (TCP) socket. The WebSocket API is being standardized by the W3C, and the WebSocket protocol has been standardized by the IETF as RFC 6455.
>
>WebSocket is designed to be implemented in web browsers and web servers, but it can be used by any client or server application. Because ordinary TCP connections to port numbers other than 80 are frequently blocked by administrators outside of home environments, it can be used as a way to circumvent these restrictions and provide similar functionality with some additional protocol overhead while multiplexing several WebSocket services over a single TCP port.
>
>WebSocket is also useful for web applications that require real-time bi-directional communication. Before the implementation of WebSocket, such bi-directional communication was only possible using Comet channels; however, Comet is not trivial to implement reliably, and due to the TCP handshaking and HTTP header overhead, it may be inefficient for small messages. The WebSocket protocol aims to solve these problems without compromising the web’s security assumptions.
>
> <http://en.wikipedia.org/wiki/WebSocket>

## Handling WebSockets

Until now, we were using `Action` instances to handle standard HTTP requests and send back standard HTTP responses. WebSockets are a totally different beast and can’t be handled via standard `Action`.

To handle a WebSocket request, use a `WebSocket` instead of an `Action`:

```scala
def index = WebSocket.using[String] { request => 
  
  // Log events to the console
  val in = Iteratee.foreach[String](println).map { _ =>
    println("Disconnected")
  }
  
  // Send a single 'Hello!' message
  val out = Enumerator("Hello!")
  
  (in, out)
}
```

A `WebSocket` has access to the request headers (from the HTTP request that initiates the WebSocket connection), allowing you to retrieve standard headers and session data. However, it doesn’t have access to a request body, nor to the HTTP response.

When constructing a `WebSocket` this way, we must return both `in` and `out` channels.

- The `in` channel is an `Iteratee[A,Unit]` (where `A` is the message type - here we are using `String`) that will be notified for each message, and will receive `EOF` when the socket is closed on the client side.
- The `out` channel is an `Enumerator[A]` that will generate the messages to be sent to the Web client. It can close the connection on the server side by sending `EOF`.

It this example we are creating a simple iteratee that prints each message to console. To send messages, we create a simple dummy enumerator that will send a single **Hello!** message.

> **Tip:** You can test WebSockets on <http://websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that discards the input data and closes the socket just after sending the **Hello!** message:

```scala
def index = WebSocket.using[String] { request => 
  
  // Just consume and ignore the input
  val in = Iteratee.consume[String]()
  
  // Send a single 'Hello!' message and close
  val out = Enumerator("Hello!").andThen(Enumerator.eof)
  
  (in, out)
}
```

Here is another example in which the input data is logged to standard out and broadcast by to the client utilizing 'Concurrent.broadcast'.

```scala
//This shows an updated websocket example for play 2.2.0 utilizing Concurrent.broadcast vs Enumerator.imperative, which is now deprecated.

 def index =  WebSocket.using[String] { request =>
 
   //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[String]
 
    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[String] {
      msg => println(msg)
             //the Enumerator returned by Concurrent.broadcast subscribes to the channel and will 
             //receive the pushed messages
             channel push("RESPONSE: " + msg)
    }
    (in,out)
}
```

> **Next:** [[The template engine | ScalaTemplates]]
