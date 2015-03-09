<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Manipulating the response

## Changing the default Content-Type

The result content type is automatically inferred from the Java value you specify as body.

For example:

@[text-content-type](code/javaguide/http/JavaResponse.java)

Will automatically set the `Content-Type` header to `text/plain`, while:

@[json-content-type](code/javaguide/http/JavaResponse.java)

will set the `Content-Type` header to `application/json`.

This is pretty useful, but sometimes you want to change it. Just use the `as(newContentType)` method on a result to create a new similar result with a different `Content-Type` header:

@[custom-content-type](code/javaguide/http/JavaResponse.java)

You can also set the content type on the HTTP response context:

@[context-content-type](code/javaguide/http/JavaResponse.java)

## Setting HTTP response headers

@[response-headers](code/javaguide/http/JavaResponse.java)

Note that setting an HTTP header will automatically discard any previous value.

## Setting and discarding cookies

Cookies are just a special form of HTTP headers, but Play provides a set of helpers to make it easier.

You can easily add a Cookie to the HTTP response:

@[set-cookie](code/javaguide/http/JavaResponse.java)

If you need to set more details, including the path, domain, expiry, whether it's secure, and whether the HTTP only flag should be set, you can do this with the overloaded methods:

@[detailed-set-cookie](code/javaguide/http/JavaResponse.java)

To discard a Cookie previously stored on the web browser:

@[discard-cookie](code/javaguide/http/JavaResponse.java)

If you set a path or domain when setting the cookie, make sure that you set the same path or domain when discarding the cookie, as the browser will only discard it if the name, path and domain match.

## Specifying the character encoding for text results

For a text-based HTTP response it is very important to handle the character encoding correctly. Play handles that for you and uses `utf-8` by default.

The encoding is used to both convert the text response to the corresponding bytes to send over the network socket, and to add the proper `;charset=xxx` extension to the `Content-Type` header.

The encoding can be specified when you are generating the `Result` value:

@[charset](code/javaguide/http/JavaResponse.java)
