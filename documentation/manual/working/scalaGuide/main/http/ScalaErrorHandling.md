<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling errors

There are two main types of errors that an HTTP application can return - client errors and server errors.  Client errors indicate that the connecting client has done something wrong, server errors indicate that there is something wrong with the server.

Play will in many circumstances automatically detect client errors - these include errors such as malformed header values, unsupported content types, and requests for resources that can't be found.  Play will also in many circumstances automatically handle server errors - if your action code throws an exception, Play will catch this and generate a server error page to send to the client.

The interface through which Play handles these errors is [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html).  It defines two methods, `onClientError`, and `onServerError`.

## Handling errors in a JSON API

By default, Play returns errors in a HTML format.
For a JSON API, it's more consistent to return errors in JSON.

Play proposes an alternative `HttpErrorHandler` implementation, named [`JsonHttpErrorHandler`](api/scala/play/api/http/JsonHttpErrorHandler.html), which will return errors formatted in JSON.

To use that `HttpErrorHandler` implementation, you should configure the `play.http.errorHandler` configuration property in `application.conf` like this:

    play.http.errorHandler = play.api.http.JsonHttpErrorHandler

## Using both HTML and JSON, and other content types

If your application uses a mixture of HTML and JSON, as is common in modern web apps, Play offers another error handler that delegates to either the HTML or JSON error handler based on the preferences specified in the client's `Accept` header. This can be specified with:

    play.http.errorHandler = play.api.http.HtmlOrJsonHttpErrorHandler

This is a suitable default choice of error handler for most applications.

Finally, if you want to support other content types for errors in addition to HTML and JSON, you can extend [`PreferredMediaTypeHttpErrorHandler`](api/scala/play/api/http/PreferredMediaTypeHttpErrorHandler.html) and specify a custom error handler as described below.

## Supplying a custom error handler

If you're using [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to construct your app, override the `httpErrorHandler` method to return an instance of your custom handler.

If you're using runtime dependency injection (e.g. Guice), the error handler can be dynamically loaded at runtime. The simplest way is to create a class in the root package called `ErrorHandler` that implements [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), for example:

@[root](code/ScalaErrorHandling.scala)

If you place your error handler in the root package (i.e. package-less) and name it `ErrorHandler`, Play will use it by default.

But, in case you want to:

- Add it inside a package;
- Configure different error handlers for different environments;

Then add in `application.conf` the configuration property `play.http.errorHandler` pointing to your custom error handler class:

    play.http.errorHandler = "com.example.ErrorHandler"

If you want to use the error handler for the client's preferred media type and add your own error handler for another media type, you can extend the [`PreferredMediaTypeHttpErrorHandler`](api/scala/play/api/http/PreferredMediaTypeHttpErrorHandler.html):

@[custom-media-type](code/ScalaErrorHandling.scala)

The above example uses the default Play handlers for JSON and HTML and adds a custom handler that will be used if the client prefers plain text, e.g. if the request has `Accept: text/plain`.

## Extending the default error handler

Out of the box, Play's default error handler provides a lot of useful functionality.  For example, in dev mode, when a server error occurs, Play will attempt to locate and render the piece of code in your application that caused that exception, so that you can quickly see and identify the problem.  You may want to provide custom server errors in production, while still maintaining that functionality in development.  To facilitate this, Play provides a [`DefaultHttpErrorHandler`](api/scala/play/api/http/DefaultHttpErrorHandler.html) that has some convenience methods that you can override so that you can mix in your custom logic with Play's existing behavior.

For example, to just provide a custom server error message in production, leaving the development error message untouched, and you also wanted to provide a specific forbidden error page:

@[default](code/ScalaErrorHandling.scala)

Check out the full API documentation for [`DefaultHttpErrorHandler`](api/scala/play/api/http/DefaultHttpErrorHandler.html) to see what methods are available to override, and how you can take advantage of them.
