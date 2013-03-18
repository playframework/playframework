# The Play WS API

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its `play.api.libs.ws.WS` library, which provides a way to make asynchronous HTTP calls.

Any calls made by `play.api.libs.ws.WS` should return a `scala.concurrent.Future[play.api.libs.ws.Response]` which we can later handle with Play’s asynchronous mechanisms.

## Making an HTTP call

To send an HTTP request you start with `WS.url()` to specify the URL. Then you get a builder that you can use to specify various HTTP options, such as setting headers. You end by calling a final method corresponding to the HTTP method you want to use. For example:

```scala
val homePage: Future[play.api.libs.ws.Response] = WS.url("http://mysite.com").get()
```

Or:

```scala
val result: Future[ws.Response] = {
  WS.url("http://localhost:9001/post").post("content")
}
```


## Retrieving the HTTP response result

The call is asynchronous and you need to manipulate it as a `Promise[ws.Response]` to get the actual content. You can compose several promises and end with a `Promise[Result]` that can be handled directly by the Play server:

```scala
def feedTitle(feedUrl: String) = Action {
  Async {
    WS.url(feedUrl).get().map { response =>
      Ok("Feed title: " + (response.json \ "title").as[String])
    }
  }  
}
```

## Post url-form-encoded data

To post url-form-encoded data a `Map[String, Seq[String]]` needs to be passed into post()

```scala
WS.url(url).post(Map("key" -> Seq("value")))
```

## Configuring WS client

Use the following properties to configure the WS client

* `ws.timeout` sets both the connection and request timeout in milliseconds
* `ws.followRedirects` configures the client to follow 301 and 302 redirects
* `ws.useProxyProperties`to use the system http proxy settings(http.proxyHost, http.proxyPort) 
* `ws.useragent` to configure the User-Agent header field
* `ws.acceptAnyCertificate` set it to false to use the default SSLContext

You can also get access to the underlying client using `def client` method  
 
### Timeouts

There is 3 different timeouts in WS. Reaching a timeout causes the WS request to interrupt.

* **Connection Timeout**: The maximum time to wait when connecting to the remote host *(default is **120 seconds**)*.
* **Connection Idle Timeout**: The maximum time the request can stay idle (connexion is established but waiting for more data) *(default is **120 seconds**)*.
* **Request Timeout**: The total time you accept a request to take (it will be interrupted, whatever if the remote host is still sending data) *(default is **none**, to allow stream consuming)*.

You can define each timeout in `application.conf` with respectively: `ws.timeout.connection`, `ws.timeout.idle`, `ws.timeout.request`.

Alternatively, `ws.timeout` can be defined to target both *Connection Timeout* and *Connection Idle Timeout*.

The request timeout can be specified for a given connection with `withRequestTimeout`.

Example:

```scala
WS.url("http://playframework.org/").withRequestTimeout(10000 /* in milliseconds */)
```

> **Next:** [[OpenID Support in Play | ScalaOpenID]]
