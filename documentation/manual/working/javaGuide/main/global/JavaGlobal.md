<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Application global settings

## The Global object

Defining a `Global` class in your project allows you to handle global settings for your application:

@[global](code/javaguide/global/simple/Global.java)

By default, this object is defined in the root package, but you can define it wherever you want and then configure it in your `application.conf` using `application.global` property.

## Intercepting application start-up and shutdown

You can override the `onStart` and `onStop` operation to be notified of the corresponding application lifecycle events:

@[global](code/javaguide/global/startstop/Global.java)
