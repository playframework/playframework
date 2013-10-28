# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the default (empty) package and must extend [`GlobalSettings`](api/scala/index.html#play.api.GlobalSettings).

@[global-define](code/ScalaGlobal.scala)

> **Tip:** You can also specify a custom `GlobalSettings` implementation class name using the `application.global` configuration key.

## Hooking into application start and stop events

You can override the `onStart` and `onStop` methods to be notified of the events in the application life-cycle:

@[global-hooking](code/ScalaGlobal.scala)

## Providing an application error page

When an exception occurs in your application, the `onError` operation will be called. The default is to use the internal framework error page:

@[global-hooking-error](code/ScalaGlobal.scala)

## Handling missing actions and binding errors

If the framework doesn’t find an `Action` for a request, the `onHandlerNotFound` operation will be called:

@[global-hooking-notfound](code/ScalaGlobal.scala)


The `onBadRequest` operation will be called if a route was found, but it was not possible to bind the request parameters:

@[global-hooking-bad-request](code/ScalaGlobal.scala)

> **Next:** [[Intercepting requests | ScalaInterceptors]]
