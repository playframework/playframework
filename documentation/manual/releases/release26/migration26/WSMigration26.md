<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
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

The `WSAPI` class has been removed.  The `WSClient` interface is the point of entry for the WS API.

`WSRequest` had a `withBody[T](body: T)(implicit writable: play.api.http.Writable[T])` method has been replaced as it was difficult to track the behavior of `Writable`. There is now a custom `BodyWritable[T]` type class that fills the same function, and which has type class instances defined in Standalone WS:

```scala
override def withBody[T: BodyWritable](body: T)
```

The deprecated Scala singleton object `play.api.libs.ws.WS` has been removed.  An instance of `WSClient` should be used instead.  If compile time dependency injection is being used, then the `AhcWSComponents` trait should be mixed in.

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

The `ning` package has been replaced by the `ahc` package, and the Ning* classes replaced by AHC*.

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
