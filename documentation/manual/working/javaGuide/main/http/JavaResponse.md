<!--- Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com> -->
# Manipulating Results

## Changing the default `Content-Type`

The result content type is automatically inferred from the Java value you specify as response body.

For example:

@[text-content-type](code/javaguide/http/JavaResponse.java)

Will automatically set the `Content-Type` header to `text/plain`, while:

@[json-content-type](code/javaguide/http/JavaResponse.java)

will set the `Content-Type` header to `application/json`.

This is pretty useful, but sometimes you want to change it. Just use the `as(newContentType)` method on a result to create a new similar result with a different `Content-Type` header:

@[custom-content-type](code/javaguide/http/JavaResponse.java)

or even better, using:

@[content-type_defined_html](code/javaguide/http/JavaResponse.java)

## Manipulating HTTP headers

You can also add (or update) any HTTP header to the result:

@[response-headers](code/javaguide/http/JavaResponse.java)

Note that setting an HTTP header will automatically discard the previous value if it was existing in the original result.

## Setting and discarding cookies

Cookies are just a special form of HTTP headers, but Play provides a set of helpers to make it easier.

You can easily add a Cookie to the HTTP response using:

@[set-cookie](code/javaguide/http/JavaResponse.java)

If you need to set more details, including the path, domain, expiry, whether it's secure, and whether the HTTP only flag should be set, you can do this with the overloaded methods:

@[detailed-set-cookie](code/javaguide/http/JavaResponse.java)

To discard a Cookie previously stored on the web browser:

@[discard-cookie](code/javaguide/http/JavaResponse.java)

If you set a path or domain when setting the cookie, make sure that you set the same path or domain when discarding the cookie, as the browser will only discard it if the name, path and domain match.

## Changing the charset for text based HTTP responses

For a text based HTTP response it is very important to handle the charset correctly. Play handles that for you and uses `utf-8` by default (see [why to use utf-8](http://www.w3.org/International/questions/qa-choosing-encodings#useunicode)).

The charset is used to both convert the text response to the corresponding bytes to send over the network socket, and to update the `Content-Type` header with the proper `;charset=xxx` extension.

The encoding can be specified when you are generating the `Result` value:

@[charset](code/javaguide/http/JavaResponse.java)

## Range Results

Play supports part of [RFC 7233](https://tools.ietf.org/html/rfc7233) which defines how range requests and partial responses works. It enables you to delivery a `206 Partial Content` if a satisfiable `Range` header is present in the request. It will also returns a `Accept-Ranges: bytes` for the delivered `Result`.

> **Note:** Besides the fact that some parsing is done to better handle multiple ranges, `multipart/byteranges` is not fully supported yet.

Range results can be generated for a `Source`, `InputStream`, File, and `Path`. See [`RangeResult`](api/java/play/mvc/RangeResults.html) API documentation for see all the methods available. For example:

@[range-result-input-stream](code/javaguide/http/JavaResponse.java)

Or for an `Source`:

@[range-result-source](code/javaguide/http/JavaResponse.java)

When the request `Range` is not satisfiable, for example, if the range in the request's `Range` header field do not overlap the current extent of the selected resource, then a HTTP status `416` (Range Not Satisfiable) is returned.

It is also possible to pre-seek for a specific position of the `Source` to more efficiently deliver range results. To do that, you can provide a function where the pre-seek happens:

@[range-result-source-with-offset](code/javaguide/http/JavaResponse.java)
