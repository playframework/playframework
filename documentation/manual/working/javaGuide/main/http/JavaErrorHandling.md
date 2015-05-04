<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling errors

There are two main types of errors that an HTTP application can return - client errors and server errors.  Client errors indicate that the connecting client has done something wrong, server errors indicate that there is something wrong with the server.

Play will in many circumstances automatically detect client errors - these include errors such as malformed header values, unsupported content types, and requests for resources that can't be found.  Play will also in many circumstances automatically handle server errors - if your action code throws an exception, Play will catch this and generate a server error page to send to the client.

The interface through which Play handles these errors is [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html).  It defines two methods, `onClientError`, and `onServerError`.

## Supplying a custom error handler

A custom error handler can be supplied by creating a class in the root package called `ErrorHandler` that implements [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), for example:

@[root](code/javaguide/http/root/ErrorHandler.java)

If you don't want to place your error handler in the root package, or if you want to be able to configure different error handlers for different environments, you can do this by configuring the `play.http.errorHandler` configuration property in `application.conf`:

    play.http.errorHandler = "com.example.ErrorHandler"

## Extending the default error handler

Out of the box, Play's default error handler provides a lot of useful functionality.  For example, in dev mode, when a server error occurs, Play will attempt to locate and render the piece of code in your application that caused that exception, so that you can quickly see and identify the problem.  You may want to provide custom server errors in production, while still maintaining that functionality in development.  To facilitate this, Play provides a [`DefaultHttpErrorHandler`](api/java/play/http/DefaultHttpErrorHandler.html) that has some convenience methods that you can override so that you can mix in your custom logic with Play's existing behavior.

For example, to just provide a custom server error message in production, leaving the development error message untouched, and you also wanted to provide a specific forbidden error page:

@[default](code/javaguide/http/def/ErrorHandler.java)

Checkout the full API documentation for [`DefaultHttpErrorHandler`](api/java/play/http/DefaultHttpErrorHandler.html) to see what methods are available to override, and how you can take advantage of them.