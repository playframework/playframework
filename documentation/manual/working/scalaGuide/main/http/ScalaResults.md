<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Manipulating Results

## Changing the default `Content-Type`

The result content type is automatically inferred from the Scala value that you specify as the response body.

For example:

@[content-type_text](code/ScalaResults.scala)

Will automatically set the `Content-Type` header to `text/plain`, while:

@[content-type_xml](code/ScalaResults.scala)

will set the Content-Type header to `application/xml`.

> **Tip:** this is done via the `play.api.http.ContentTypeOf` type class.

This is pretty useful, but sometimes you want to change it. Just use the `as(newContentType)` method on a result to create a new similar result with a different `Content-Type` header:

@[content-type_html](code/ScalaResults.scala)

or even better, using:

@[content-type_defined_html](code/ScalaResults.scala)

> **Note:** The benefit of using `HTML` instead of the `"text/html"` is that the charset will be automatically handled for you and the actual Content-Type header will be set to `text/html; charset=utf-8`. We will [[see that in a bit|ScalaResults#Changing-the-charset-for-text-based-HTTP-responses]].

## Manipulating HTTP headers

You can also add (or update) any HTTP header to the result:

@[set-headers](code/ScalaResults.scala)

Note that setting an HTTP header will automatically discard the previous value if it was existing in the original result.

## Setting and discarding cookies

Cookies are just a special form of HTTP headers but we provide a set of helpers to make it easier.

You can easily add a Cookie to the HTTP response using:

@[set-cookies](code/ScalaResults.scala)

Also, to discard a Cookie previously stored on the Web browser:

@[discarding-cookies](code/ScalaResults.scala)

You can also set and remove cookies as part of the same response:

@[setting-discarding-cookies](code/ScalaResults.scala)

## Changing the charset for text based HTTP responses

For a text based HTTP response it is very important to handle the charset correctly. Play handles that for you and uses `utf-8` by default (see [why to use utf-8](http://www.w3.org/International/questions/qa-choosing-encodings#useunicode)).

The charset is used to both convert the text response to the corresponding bytes to send over the network socket, and to update the `Content-Type` header with the proper `;charset=xxx` extension.

The charset is handled automatically via the `play.api.mvc.Codec` type class. Just import an implicit instance of `play.api.mvc.Codec` in the current scope to change the charset that will be used by all operations:

@[full-application-set-myCustomCharset](code/ScalaResults.scala)

Here, because there is an implicit charset value in the scope, it will be used by both the `Ok(...)` method to convert the XML message into `ISO-8859-1` encoded bytes and to generate the `text/html; charset=iso-8859-1` Content-Type header.

Now if you are wondering how the `HTML` method works, here it is how it is defined:

@[Source-Code-HTML](code/ScalaResults.scala)

You can do the same in your API if you need to handle the charset in a generic way.

## Range Results

Play supports part of [RFC 7233](https://tools.ietf.org/html/rfc7233) which defines how range requests and partial responses works. It enables you to deliver a `206 Partial Content` if a satisfiable `Range` header is present in the request. It will also returns a `Accept-Ranges: bytes` for the delivered `Result`.

> **Note:** Besides the fact that some parsing is done to better handle multiple ranges, `multipart/byteranges` is not fully supported yet.

Range results can be generated for a `Source`, `InputStream`, File, and `Path`. See [`RangeResult`](api/scala/play/api/mvc/RangeResult$.html) API documentation for see all the methods available. For example:

@[range-result-input-stream](code/ScalaResults.scala)

Or for an `Source`:

@[range-result-source](code/ScalaResults.scala)

When the request `Range` is not satisfiable, for example, if the range in the request's `Range` header field do not overlap the current extent of the selected resource, then a HTTP status `416` (Range Not Satisfiable) is returned.

It is also possible to pre-seek for a specific position of the `Source` to more efficiently deliver range results. To do that, you can provide a function where the pre-seek happens:

@[range-result-source-with-offset](code/ScalaResults.scala)
