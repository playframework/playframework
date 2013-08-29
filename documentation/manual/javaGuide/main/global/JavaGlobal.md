# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the root package.

@[global](code/javaguide/global/simple/Global.java)

## Intercepting application start-up and shutdown

You can override the `onStart` and `onStop` operation to be notified of the corresponding application lifecycle events:

@[global](code/javaguide/global/startstop/Global.java)

## Providing an application error page

When an exception occurs in your application, the `onError` operation will be called. The default is to use the internal framework error page. You can override this:

@[global](code/javaguide/global/onerror/Global.java)

## Handling action not found

If the framework doesn’t find an action method for a request, the `onHandlerNotFound` operation will be called:

@[global](code/javaguide/global/notfound/Global.java)

The `onBadRequest` operation will be called if a route was found, but it was not possible to bind the request parameters:

@[global](code/javaguide/global/badrequest/Global.java)

> **Next:** [[Intercepting requests | JavaInterceptors]]
