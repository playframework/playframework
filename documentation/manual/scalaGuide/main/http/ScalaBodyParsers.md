# Body parsers

## What is a body parser?

An HTTP PUT or POST request contains a body. This body can use any format, specified in the `Content-Type` request header. In Play, a **body parser** transforms this request body into a Scala value. 

However the request body for an HTTP request can be very large and a **body parser** can’t just wait and load the whole data set into memory before parsing it. A `BodyParser[A]` is basically an `Iteratee[Array[Byte],A]`, meaning that it receives chunks of bytes (as long as the web browser uploads some data) and computes a value of type `A` as result.

Let’s consider some examples.

- A **text** body parser could accumulate chunks of bytes into a String, and give the computed String as result (`Iteratee[Array[Byte],String]`).
- A **file** body parser could store each chunk of bytes into a local file, and give a reference to the `java.io.File` as result (`Iteratee[Array[Byte],File]`).
- A **s3** body parser could push each chunk of bytes to Amazon S3 and give a the S3 object id as result (`Iteratee[Array[Byte],S3ObjectId]`).

Additionally a **body parser** has access to the HTTP request headers before it starts parsing the request body, and has the opportunity to run some precondition checks. For example, a body parser can check that some HTTP headers are properly set, or that the user trying to upload a large file has the permission to do so.

> **Note**: That's why a body parser is not really an `Iteratee[Array[Byte],A]` but more precisely a `Iteratee[Array[Byte],Either[Result,A]]`, meaning that it has the opportunity to send directly an HTTP result itself (typically `400 BAD_REQUEST`, `412 PRECONDITION_FAILED` or `413 REQUEST_ENTITY_TOO_LARGE`) if it decides that it is not able to compute a correct value for the request body

Once the body parser finishes its job and gives back a value of type `A`, the corresponding `Action` function is executed and the computed body value is passed into the request.

## More about Actions

Previously we said that an `Action` was a `Request => Result` function. This is not entirely true. Let’s have a more precise look at the `Action` trait:

```scala
trait Action[A] extends (Request[A] => Result) {
  def parser: BodyParser[A]
}
```

First we see that there is a generic type `A`, and then that an action must define a `BodyParser[A]`. With `Request[A]` being defined as:

```scala
trait Request[+A] extends RequestHeader {
  def body: A
}
```

The `A` type is the type of the request body. We can use any Scala type as the request body, for example `String`, `NodeSeq`, `Array[Byte]`, `JsonValue`, or `java.io.File`, as long as we have a body parser able to process it.

To summarize, an `Action[A]` uses a `BodyParser[A]` to retrieve a value of type `A` from the HTTP request, and to build a `Request[A]` object that is passed to the action code. 

## Default body parser: AnyContent

In our previous examples we never specified a body parser. So how can it work? If you don’t specify your own body parser, Play will use the default, which processes the body as an instance of `play.api.mvc.AnyContent`.

This body parser checks the `Content-Type` header and decides what kind of body to process:

- **text/plain**: `String`
- **application/json**: `JsValue`
- **text/xml**: `NodeSeq`
- **application/form-url-encoded**: `Map[String, Seq[String]]`
- **multipart/form-data**: `MultipartFormData[TemporaryFile]`
- any other content type: `RawBuffer`

For example:

```scala
def save = Action { request =>
  val body: AnyContent = request.body
  val textBody: Option[String] = body.asText 
  
  // Expecting text body
  textBody.map { text =>
    Ok("Got: " + text)
  }.getOrElse {
    BadRequest("Expecting text/plain request body")  
  }
}
```

## Specifying a body parser

The body parsers available in Play are defined in `play.api.mvc.BodyParsers.parse`.

So for example, to define an action expecting a text body (as in the previous example):

```scala
def save = Action(parse.text) { request => 
   Ok("Got: " + request.body) 
} 
```

Do you see how the code is simpler? This is because the `parse.text` body parser already sent a `400 BAD_REQUEST` response if something went wrong. We don’t have to check again in our action code, and we can safely assume that `request.body` contains the valid `String` body.

Alternatively we can use:

```scala
def save = Action(parse.tolerantText) { request =>
  Ok("Got: " + request.body)
}
```

This one doesn't check the `Content-Type` header and always loads the request body as a `String`.

> **Tip:** There is a `tolerant` fashion provided for all body parsers included in Play.

Here is another example, which will store the request body in a file:

```scala
def save = Action(parse.file(to = new File("/tmp/upload"))) { request =>
  Ok("Saved the request content to " + request.body)
}
```

## Combining body parsers

In the previous example, all request bodies are stored in the same file. This is a bit problematic isn’t it? Let’s write another custom body parser that extract the user name from the request Session, to give a unique file for each user:

```scala
val storeInUserFile = parse.using { request =>
  request.session.get("username").map { user =>
    file(to = new File("/tmp/" + user + ".upload"))
  }.getOrElse {
    error(Unauthorized("You don't have the right to upload here"))
  }
}

def save = Action(storeInUserFile) { request =>
  Ok("Saved the request content to " + request.body)  
}
```

> **Note:** Here we are not really writing our own BodyParser, but just combining existing ones. This is often enough and should cover most use cases. Writing a `BodyParser` from scratch is covered in the advanced topics section.

## Max content length

Text based body parsers (such as **text**, **json**, **xml** or **formUrlEncoded**) use a maximum content length because they have to load all of the content into memory. 

There is a default content length (the default is 100KB), but you can also specify it inline:

```scala
// Accept only 10KB of data.
def save = Action(parse.text(maxLength = 1024 * 10)) { request =>
  Ok("Got: " + text)
}
```

> **Tip:** The default content size can be defined in `application.conf`:
> 
> `parsers.text.maxLength=128K`

You can also wrap any body parser with `maxLength`:

```scala
// Accept only 10KB of data.
def save = Action(maxLength(1024 * 10, parser = storeInUserFile)) { request =>
  Ok("Saved the request content to " + request.body)  
}
```

> **Next:** [[Action composition | ScalaActionsComposition]]