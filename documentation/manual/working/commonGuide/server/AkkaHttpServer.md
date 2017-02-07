<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Akka HTTP Server Backend

Play uses the [Akka HTTP](http://doc.akka.io/docs/akka-http/current/) server backend to implement HTTP requests and responses using Akka Streams over the network.  Akka HTTP implements a full server stack for HTTP, including full HTTPS support, and has support for HTTP/2.

## Akka HTTP Implementation

Play's server backend uses the [low level server API](http://doc.akka.io/docs/akka-http/current/scala/http/low-level-server-side-api.html) to handle Akka's `HttpRequest` and `HttpResponse` classes.

Play's server backend automatically converts of an Akka `HttpRequest` into a Play HTTP request, so that details of the implementation are under the hood.  Play handles all the routing and application logic surrounding the server backend, while still providing the power and reliability of Akka-HTTP for processing requests.

## Working with Blocking APIs

Like the rest of Play, Akka HTTP is non-blocking.  This means that it uses a small number of threads which it keeps loaded with work at all times.

This poses a problem when working with blocking APIs such as JDBC or HTTPURLConnection, which cause a thread to wait until data has been returned from a remote system.

Please configure any work with blocking APIs off the main rendering thread, using a `Future` or `CompletionStage` configured with a `CustomExecutionContext` and using a custom thread pool defined in [[ThreadPools]].  See [[JavaAsync]] and [[ScalaAsync]] for more details.

## Configuring Akka HTTP

You can configure the Akka HTTP server settings through [[application.conf|SettingsAkkaHttp]].