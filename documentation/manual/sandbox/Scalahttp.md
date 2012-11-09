# Making HTTP Requests

Sometimes we would like to call other HTTP services from within a play application. Play supports this task via its ```play.api.libs.WS``` library which provides a way to make asynchronous HTTP calls. 

Any calls made by ```play.api.libs.WS``` should return a ```play.api.libs.concurrent.Promise[play.api.libs.ws.Request]``` which we can later handle with play's asynchronous mechanisms.

# A short introduction

## Hello Get
```scala
import play.api.libs._
//let's try to retrive the value from the promise within 5sec
val myfeed: Promise[ws.Response] = WS.url("http://mysite.com").get()

val content: String = myfeed.await(5000).get.body

val content: xml.Elem = myfeed.await(5000).get.xml

val content: JsValue = myfeed.await(5000).get.json

val code = myfeed.await(5000).get.status
```

## Hello Post
```scala

//send data as text
val content: String = WS.url("http://localhost:9001/post").post("content").await(5000).get.body
content must contain ("content")
content must contain("AnyContentAsText")

//or as x-www-application/form-urlencoded
val contentForm: String = WS.url("http://localhost:9001/post").post(Map("param1"->Seq("foo"))).await(5000).get.body
contentForm must contain ("AnyContentAsUrlFormEncoded")
contentForm must contain ("foo")
```

one also can add custom request and authentication headers and even a signature. For more information please see the api doc [here](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/libs/WS.scala)

