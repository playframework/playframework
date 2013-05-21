# WebSockets

## Using WebSockets instead of Comet sockets

A Comet socket is a kind of hack for sending live events to the web browser. Also, Comet only supports one-way communication from the server to the client. To push events to the server, the web browser can make Ajax requests.

Modern web browsers natively support two-way live communication via WebSockets.

>WebSocket is a web technology providing for bi-directional, full-duplex communications channels, over a single Transmission Control Protocol (TCP) socket. The WebSocket API is being standardized by the W3C, and the WebSocket protocol has been standardized by the IETF as RFC 6455.
>
>WebSocket is designed to be implemented in web browsers and web servers, but it can be used by any client or server application. Because ordinary TCP connections to port numbers other than 80 are frequently blocked by administrators outside of home environments, it can be used as a way to circumvent these restrictions and provide similar functionality with some additional protocol overhead while multiplexing several WebSocket services over a single TCP port. Additionally, it serves a purpose for web applications that require real-time bi-directional communication. Before the implementation of WebSocket, such bi-directional communication was only possible using comet channels; however, a comet is not trivial to implement reliably, and due to the TCP Handshake and HTTP header overhead, it may be inefficient for small messages. The WebSocket protocol aims to solve these problems without compromising security assumptions of the web.
>
> <http://en.wikipedia.org/wiki/WebSocket>

## Handling WebSockets

Until now we were using a simple action method to handle standard HTTP requests and send back standard HTTP results. WebSockets are a totally different beast, and can’t be handled via standard actions.

To handle a WebSocket your method must return a `WebSocket` instead of a `Result`:

@[websocket](code/javaguide/async/JavaWebSockets.java)

A WebSocket has access to the request headers (from the HTTP request that initiates the WebSocket connection) allowing you to retrieve standard headers and session data. But it doesn't have access to any request body, nor to the HTTP response.

When the `WebSocket` is ready, you get both `in` and `out` channels.

It this example, we print each message to console and we send a single **Hello!** message.

> **Tip:** You can test your WebSocket controller on <http://websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that totally discards the input data and closes the socket just after sending the **Hello!** message:

@[discard-input](code/javaguide/async/JavaWebSockets.java)

> **Next:** [[The template engine | JavaTemplates]]
