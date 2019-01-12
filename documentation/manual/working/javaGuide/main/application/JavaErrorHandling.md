<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling errors

There are two main types of errors that an HTTP application can return - client errors and server errors.  Client errors indicate that the connecting client has done something wrong, server errors indicate that there is something wrong with the server.

Play will in many circumstances automatically detect client errors - these include errors such as malformed header values, unsupported content types, and requests for resources that can't be found.  Play will also in many circumstances automatically handle server errors - if your action code throws an exception, Play will catch this and generate a server error page to send to the client.

The interface through which Play handles these errors is [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html).  It defines two methods, `onClientError`, and `onServerError`.

## Handling errors in a JSON API

By default, Play returns errors in a HTML format. 
For a JSON API, it's more consistent to return errors in JSON.

Play proposes an alternative `HttpErrorHandler` implementation, named [`JsonHttpErrorHandler`](api/java/play/http/JsonHttpErrorHandler.html), which will return errors formatted in JSON.

To use that `HttpErrorHandler` implementation, you should configure the `play.http.errorHandler` configuration property in `application.conf` like this:

    play.http.errorHandler = play.http.JsonHttpErrorHandler

## Using both HTML and JSON, and other content types

If your application uses a mixture of HTML and JSON, as is common in modern web apps, Play offers another error handler that delegates to either the HTML or JSON error handler based on the preferences specified in the client's `Accept` header. This can be specified with:

    play.http.errorHandler = play.http.HtmlOrJsonHttpErrorHandler

This is a suitable default choice of error handler for most applications.

Finally, if you want to support other content types for errors in addition to HTML and JSON, you can extend [`PreferredMediaTypeHttpErrorHandler`](api/java/play/http/PreferredMediaTypeHttpErrorHandler.html) and add error handlers for specific content types, then specify a custom error handler as described below.

## Supplying a custom error handler

If you're using [`BuiltInComponents`](api/java/play/BuiltInComponents.html) to construct your app, override the `httpRequestHandler` method to return an instance of your custom handler.

If you're using runtime dependency injection (e.g. Guice), the error handler can be dynamically loaded at runtime. The simplest way is to create a class in the root package called `ErrorHandler` that implements [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), for example:

@[root](code/javaguide/application/root/ErrorHandler.java)

If you don't want to place your error handler in the root package, or if you want to be able to configure different error handlers for different environments, you can do this by configuring the `play.http.errorHandler` configuration property in `application.conf`:

    play.http.errorHandler = "com.example.ErrorHandler"

## Extending the default error handler

Out of the box, Play's default error handler provides a lot of useful functionality.  For example, in dev mode, when a server error occurs, Play will attempt to locate and render the piece of code in your application that caused that exception, so that you can quickly see and identify the problem.  You may want to provide custom server errors in production, while still maintaining that functionality in development.  To facilitate this, Play provides a [`DefaultHttpErrorHandler`](api/java/play/http/DefaultHttpErrorHandler.html) that has some convenience methods that you can override so that you can mix in your custom logic with Play's existing behavior.

For example, to just provide a custom server error message in production, leaving the development error message untouched, and you also wanted to provide a specific forbidden error page:

@[default](code/javaguide/application/def/ErrorHandler.java)

Checkout the full API documentation for [`DefaultHttpErrorHandler`](api/java/play/http/DefaultHttpErrorHandler.html) to see what methods are available to override, and how you can take advantage of them.
