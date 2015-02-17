<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Streaming HTTP responses

## Standard responses and Content-Length header

Since HTTP 1.1, to keep a single connection open to serve several HTTP requests and responses, the server must send the appropriate `Content-Length` HTTP header along with the response. 

By default, when you send a simple result, such as:

@[by-default](code/javaguide/async/JavaStream.java)

You are not specifying a `Content-Length` header. Of course, because the content you are sending is well known, Play is able to compute the content size for you and to generate the appropriate header.

> **Note** that for text-based content this is not as simple as it looks, since the `Content-Length` header must be computed according the encoding used to translate characters to bytes.

To be able to compute the `Content-Length` header properly, Play must consume the whole response data and load its content into memory. 

## Serving files

If it’s not a problem to load the whole content into memory for simple content what about a large data set? Let’s say we want to send back a large file to the web client.

Play provides easy to use helpers to this common task of serving a local file:

@[serve-file](code/javaguide/async/JavaStream.java)

Additionally this helper will also compute the `Content-Type` header from the file name. And it will also add the `Content-Disposition` header to specify how the web browser should handle this response. The default is to ask the web browser to download this file by using `Content-Disposition: attachment; filename=fileToServe.pdf`.

## Chunked responses

For now, this works well with streaming file content, since we are able to compute the content length before streaming it. But what about dynamically-computed content with no content size available?

For this kind of response we have to use **Chunked transfer encoding**. 

> **Chunked transfer encoding** is a data transfer mechanism in version HTTP 1.1 in which a web server serves content in a series of chunks. This uses the `Transfer-Encoding` HTTP response header instead of the `Content-Length` header, which the protocol would otherwise require. Because the `Content-Length` header is not used, the server does not need to know the length of the content before it starts transmitting a response to the client (usually a web browser). Web servers can begin transmitting responses with dynamically-generated content before knowing the total size of that content.
> 
> The size of each chunk is sent right before the chunk itself so that a client can tell when it has finished receiving data for that chunk. The data transfer is terminated by a final chunk of length zero.
>
> <http://en.wikipedia.org/wiki/Chunked_transfer_encoding>

The advantage is that we can serve data **live**, meaning that we send chunks of data as soon as they are available. The drawback is that since the web browser doesn’t know the content size, it is not able to display a proper download progress bar.

Let’s say that we have a service somewhere that provides a dynamic `InputStream` that computes some data. We can ask Play to stream this content directly using a chunked response:

@[input-stream](code/javaguide/async/JavaStream.java)

You can also set up your own chunked response builder. The Play Java API supports both text and binary chunked streams (via `String` and `byte[]`):

@[chunked](code/javaguide/async/JavaStream.java)

The `onReady` method is called when it is safe to write to this stream. It gives you a `Chunks.Out` channel you can write to.

Let’s say we have an asynchronous process (like an `Actor`) somewhere pushing to this stream:

@[register-out-channel](code/javaguide/async/JavaStream.java)

We can inspect the HTTP response sent by the server:

```
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
Transfer-Encoding: chunked

4
kiki
3
foo
3
bar
0

```

We get three chunks and one final empty chunk that closes the response.
