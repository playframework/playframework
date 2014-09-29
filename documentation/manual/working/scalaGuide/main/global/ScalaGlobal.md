<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the default (empty) package and must extend [`GlobalSettings`](api/scala/index.html#play.api.GlobalSettings).

@[global-define](code/ScalaGlobal.scala)

> **Tip:** You can also specify a custom `GlobalSettings` implementation class name using the `application.global` configuration key.

## Hooking into application start and stop events

You can override the `onStart` and `onStop` methods to be notified of the events in the application life-cycle:

@[global-hooking](code/ScalaGlobal.scala)
