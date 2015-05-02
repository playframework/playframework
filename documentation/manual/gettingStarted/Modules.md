<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play Modules

At its core, Play is a very lightweight HTTP server, providing mechanisms for serving HTTP requests, but not much else. Additional functionality in Play is provided through the use of Play modules.

## What is a module?

There is no strict definition in Play of what a module is or isn't - a module could be just a library that provides some helper methods to help you do something, or it could be a full framework providing complex functionality such as user management. Some modules are built in to Play, others are written and maintained by members of the Play community.

Some modules provide components - objects that represent resources, for example a database connection.  These objects may have a lifecycle and need to be started and stopped when the application starts and stops, and they may hold some state such as a cache. Play provides a variety of mechanisms for accessing and using these components. Components are not only provided by modules, they may be provided by the application themselves.

## Accessing modules

One of the earliest decisions that you need to make when starting a new Play project is how you will access the components provided by modules. Components are accessed through the use of a dependency injection mechanism, where rather than having your components look up other components in the system, your components declare what other components they need, and the system injects those components into your components.

At its core, Play is agnostic to any particular form of dependency injection, however out of the box Play provides and we recommend that you use [Guice](https://github.com/google/guice). The remainder of this documentation will assume that this is the decision that you have made, however there will be examples of how to integrate with other dependency injection mechanisms.

You can read more about dependency injection in [[Scala|ScalaDependencyInjection]] or [[Java|JavaDependencyInjection]].
