<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Integrating with Akka Typed

## Akka Actor Typed styles

Akka's [Typed Actor API][] has two styles:

1. a ["functional programming" style][fp-style], based on defining actor `Behavior` as values, and
2. a ["object-oriented" style][oo-style], based on defining the `Behavior` as a subclass

[Typed Actor API]: https://doc.akka.io/docs/akka/2.6/typed/actors.html
[fp-style]: https://doc.akka.io/docs/akka/2.6/typed/actors.html#functional-style
[oo-style]: https://doc.akka.io/docs/akka/2.6/typed/actors.html#object-oriented-style

Here's an example of a simple actor that says hello back:

Scala FP
: @[fp-hello-actor](code/scalaguide/akka/typed/fp/HelloActor.scala)

Scala OO
: @[oo-hello-actor](code/scalaguide/akka/typed/oo/HelloActor.scala)

Java
: @[oo-hello-actor](code/javaguide/akka/typed/HelloActor.java)

Here's an example of an actor that depends on Play's `Configuration` in order to return configuration values:

Scala FP
: @[fp-configured-actor](code/scalaguide/akka/typed/fp/ConfiguredActor.scala)

Scala OO
: @[oo-configured-actor](code/scalaguide/akka/typed/oo/ConfiguredActor.scala)

Java
: @[oo-configured-actor](code/javaguide/akka/typed/ConfiguredActor.java)

## Compile-time dependency injection

Using compile-time dependency injection for Akka Typed requires creating the actor `Behavior` value and using it to spawn the actor:

Scala
: @[compile-time-di](code/scalaguide/akka/typed/fp/AppComponents.scala)

Java
: @[compile-time-di](code/javaguide/akka/typed/AppComponents.java)

## Runtime dependency injection

For runtime dependency injection use the "typed" methods in `AkkaGuiceSupport`.

Given a component in your application or system that needs injecting, like this one:

Scala
: @[main](code/scalaguide/akka/typed/fp/Main.scala)

Java
: @[main](code/javaguide/akka/typed/Main.java)

You can define a Guice `Module` defined like so:

Scala FP
: @[fp-app-module](code/scalaguide/akka/typed/fp/AppModule.scala)

Scala OO
: @[oo-app-module](code/scalaguide/akka/typed/oo/AppModule.scala)

Java
: @[oo-app-module](code/javaguide/akka/typed/AppModule.java)
