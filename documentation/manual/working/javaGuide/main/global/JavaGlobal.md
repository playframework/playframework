<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the root package.

@[global](code/javaguide/global/simple/Global.java)

## Intercepting application start-up and shutdown

You can override the `onStart` and `onStop` operation to be notified of the corresponding application lifecycle events:

@[global](code/javaguide/global/startstop/Global.java)
