<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Body parsers

## What is a body parser?

An HTTP request (at least for those using the POST and PUT operations) contains a body. This body can be formatted with any format specified in the Content-Type header. A **body parser** transforms this request body into a Java value. 

> **Note:** You can't write `BodyParser` implementation directly using Java. Because a Play `BodyParser` must handle the body content incrementally using an `Iteratee[Array[Byte], A]` it must be implemented in Scala.
>
> However Play provides default `BodyParser`s that should fit most use cases (parsing Json, Xml, Text, uploading files). And you can reuse these default parsers to create your own directly in Java; for example you can provide an RDF parsers based on the Text one.

## The `BodyParser` Java API

When working with request bodies, ensure that have the following imports in your controller:

@[imports](code/javaguide/http/JavaBodyParsers.java)

In the Java API, all body parsers must generate a `play.mvc.Http.RequestBody` value. This value computed by the body parser can then be retrieved via `request().body()`:

@[request-body](code/javaguide/http/JavaBodyParsers.java)

You can specify the `BodyParser` to use for a particular action using the `@BodyParser.Of` annotation:

@[particular-body-parser](code/javaguide/http/JavaBodyParsers.java)

## The `Http.RequestBody` API

As we just said all body parsers in the Java API will give you a `play.mvc.Http.RequestBody` value. From this body object you can retrieve the request body content in the most appropriate Java type.

> **Note:** The `RequestBody` methods like `asText()` or `asJson()` will return `null` if the parser used to compute this request body doesn't support this content type. For example in an action method annotated with `@BodyParser.Of(BodyParser.Json.class)`, calling `asXml()` on the generated body will return `null`.

## Default body parser: AnyContent

If you don't specify your own body parser, Play will use the default one guessing the most appropriate content type from the `Content-Type` header:

- **text/plain**: `String`, accessible via `asText()`
- **application/json**: `JsonNode`, accessible via `asJson()`
- **application/xml**, **text/xml** or **application/XXX+xml**: `org.w3c.Document`, accessible via `asXml()`
- **application/form-url-encoded**: `Map<String, String[]>`, accessible via `asFormUrlEncoded()`
- **multipart/form-data**: `Http.MultipartFormData`, accessible via `asMultipartFormData()`
- Any other content type: `Http.RawBuffer`, accessible via `asRaw()`

For example:

@[default-parser](code/javaguide/http/JavaBodyParsers.java)

## Max content length

Text based body parsers (such as **text**, **json**, **xml** or **formUrlEncoded**) use a max content length because they have to load all the content into memory. 

There is a default maximum content length of 100KB.  It can be overridden by specifying the `parsers.text.maxLength` property in `application.conf`:

    parsers.text.maxLength=128K

You can also specify a maximum content length via the `@BodyParser.Of` annotation:

@[max-length](code/javaguide/http/JavaBodyParsers.java)

> **Next:** [[Actions composition | JavaActionsComposition]]
