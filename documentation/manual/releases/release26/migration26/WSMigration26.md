<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play WS Migration Guide

## Package changes

Play WS historically consisted of two libraries, `ws` and `playWs`, containing the Scala and Java APIs respectively, each individually creating an AsyncHTTPClient behind the scenes.  There is now only one `play-ahc-ws` library, which contains both Scala and Java `WSClient` instances, and both point to a singleton `AsyncHttpClient` provider.

## Project changes

Play WS now exists as a Play specific wrapper on top of a standalone WS library, which does not depend on Play classes, and which uses package renamed "shaded" versions of AsyncHttpClient, Signpost, and Netty 4.0.

By providing a standalone WS version and using shaded libraries, WS is more flexible and has fewer collisions with other libraries and projects.

The Play WS API extends Standalone WS `post` with `Http.Multipart` and `Multipart` types that are only available in Play, for example:

```scala
def withBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Self 
```

Signpost OAuth has been changed so that instead of using the Commons HTTPClient OAuthProvider, it now uses the DefaultOAuthProvider, which uses HTTPURLConnection under the hood.

The standalone WS instance is available at https://github.com/playframework/play-ws and can be added to an SBT project with:

```scala
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.0.0"
```

## API changes

### Scala

#### Removed Global WS.client

The deprecated Scala singleton object `play.api.libs.ws.WS` has been removed.  An instance of `WSClient` should be used instead.  If compile time dependency injection is being used, then the `AhcWSComponents` trait should be mixed in.

The `WSAPI` class has been removed.  The `WSClient` interface is the point of entry for the WS API.

For Guice, there is a `WSClient` available in the system:

``` scala
class MyService @Inject()(ws: WSClient) {
   def call(): Unit = {     
     ws.url("http://localhost:9000/foo").get()
   }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|ScalaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.api.test.WsTestClient`, which will start up and shut down a standalone WSClient instance:

``` scala
play.api.test.WsTestClient.withClient { ws =>
  ws.url("http://localhost:9000/foo").get()
}
```

#### ning package removed

The `ning` package has been replaced by the `ahc` package, and the Ning* classes replaced by AHC*.

#### WithBody changed

`WSRequest` had a `withBody[T](body: T)(implicit writable: play.api.http.Writable[T])` method has been replaced as it was difficult to track the behavior of `Writable`. There is now a custom `BodyWritable[T]` type class that fills the same function, and which has type class instances defined in Standalone WS:

```scala
override def withBody[T: BodyWritable](body: T)
```

#### Deprecated wsCall and wsUrl

The `wsCall` and `wsUrl` methods in `play.api.test.WsTestClient` have the goal of calling "wsClient.url(s"$scheme://localhost:" + self.port + path)" -- they resolve a WSClient, and call a relative URL on a given port.

These methods relied on `Play.current` when no implicit `Application` was found and were not very flexible.  They have been deprecated in favor of `wsCall` and `wsUrl` methods in context.

You can extend `WithServer` or `WithBrowser` with `ServerWSTestMethods` to provide the `wsCall` and `wsUrl` methods directly:

```scala
"run a test" in new WithServer() with ServerWSTestMethods {
  val response = await(wsUrl("/withServer").get())
  ...
}
```

You can also extend `WithApplication` with `AppWSTestMethods` in the same way:

```scala
"run a test" in new WithServer() with AppWSTestMethods {
  val response = await(wsUrl("/withServer").get())
  ...
}
```

The `WsTestClient` trait will now automatically enrich `WSClient`, `Application`, `TestServer` types with `wsUrl` or `wsCall` methods, which can be very convenient:

```scala
class MySpec extends PlaySpecification with WsTestClient {
  "test" should {
    "call server.wsUrl on given port" in {
      ...
      val server = TestServer(someNonDefaultPort, app)
      running(server) {
        val plainRequest = server.wsUrl("/someUrl")
        ...
      }
    }
  }
}
```

In fact, anything that contains `def app: Application` will have `wsCall` and `wsUrl` methods.  If you have a `def port: play.api.test.Port` as well, then the result of that `port` method is used.

If the type does not have a port available, i.e. an `Application` or a `WSClient`, then you can still call `wsUrl` but there is an implicit port parameter that must be resolved, either from WithServer, or by using `Helpers.testServerPort`:

```scala
class MySpec extends PlaySpecification with WsTestClient {
  "test" should {
    "call app.wsUrl with explicit port" in new WithApplication() {
      val response = await(app.wsUrl("/someUrl")(Helpers.testServerPort).get())
    }
    
    "call app.wsUrl with implicit val port" in new WithApplication() {
      implicit val implicitPort = Helpers.testServerPort
      val response = await(app.wsUrl("/someUrl").get())
    }
    
    "call app.wsUrl with implicit port from WithServer scope" in new WithServer() {
      val response = await(app.wsUrl("/someUrl").get())
    }
  }
}
```

### Java

In Java, the `play.libs.ws.WS` class has been deprecated.  An injected `WSClient` instance should be used instead.

``` java
public class MyService {
     private final WSClient ws;

     @Inject
     public MyService(WSClient ws) {
         this.ws = ws;
     }

     public void call() {     
         ws.url("http://localhost:9000/foo").get();
     }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|JavaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.test.WsTestClient`, which will start up and shut down a standalone `WSClient` instance:

``` java
WSClient ws = play.test.WsTestClient.newClient(19001);
...
ws.close();
```


