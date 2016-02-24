<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Global Settings

> **Note:** The `GlobalSettings` class is deprecated in 2.5.x.  Please see the [[Removing `GlobalSettings`|GlobalSettings]] page for how to migrate away from GlobalSettings.

## The Global object

Defining a `Global` class in your project allows you to handle global settings for your application:

@[global](code/javaguide/application/simple/Global.java)

By default, this object is defined in the root package, but you can define it wherever you want and then configure it in your `application.conf` using `application.global` property.

## Intercepting application start-up and shutdown

You can override the `onStart` and `onStop` operation to be notified of the corresponding application lifecycle events:

@[global](code/javaguide/application/startstop/Global.java)

## Overriding onRequest

One important aspect of  the ```GlobalSettings``` class is that it provides a way to intercept requests and execute business logic before a request is dispatched to an action.

For example:

@[global](code/javaguide/application/intercept/Global.java)

It’s also possible to intercept a specific action method. This can be achieved via [[Action composition|JavaActionsComposition]].
