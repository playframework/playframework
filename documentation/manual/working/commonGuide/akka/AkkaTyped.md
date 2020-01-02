<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Integrating with Akka Typed

Akka 2.6 marked the new typed Actor API ("Akka Typed") as stable. The typed API is now officially the main API for Akka. In the typed API, each actor needs to declares which message type it is able to handle and the type system enforces that only messages of this type can be sent to the actor. Although Play does not fully adopt Akka Typed, we already provide some APIs to better integrate it in Play applications.

> **Note:** the Akka classic APIs are still fully supported and existing applications can continue to use them. There are no plans to deprecate or remove Akka classic API. 

## Akka Actor Typed styles

Akka's [Actor Typed API][] has two styles:

1. a ["functional programming" style][fp-style], based on defining an actor `Behavior`s with values; and
2. a ["object-oriented" style][oo-style], based on defining an actor `Behavior`s with subclasses.

[Actor Typed API]: https://doc.akka.io/docs/akka/2.6/typed/actors.html
[fp-style]: https://doc.akka.io/docs/akka/2.6/typed/actors.html#functional-style
[oo-style]: https://doc.akka.io/docs/akka/2.6/typed/actors.html#object-oriented-style

For instance, here's an example of a simple actor that says hello back:

Scala FP
: @[fp-hello-actor](code/scalaguide/akka/typed/fp/HelloActor.scala)

Scala OO
: @[oo-hello-actor](code/scalaguide/akka/typed/oo/HelloActor.scala)

Java FP
: @[fp-hello-actor](code/javaguide/akka/typed/fp/HelloActor.java)

Java OO
: @[oo-hello-actor](code/javaguide/akka/typed/oo/HelloActor.java)

While here is an example of an actor that depends on Play's `Configuration` in order to return configuration values:

Scala FP
: @[fp-configured-actor](code/scalaguide/akka/typed/fp/ConfiguredActor.scala)

Scala OO
: @[oo-configured-actor](code/scalaguide/akka/typed/oo/ConfiguredActor.scala)

Java FP
: @[fp-configured-actor](code/javaguide/akka/typed/fp/ConfiguredActor.java)

Java OO
: @[oo-configured-actor](code/javaguide/akka/typed/oo/ConfiguredActor.java)

## Dependency Injection

If your actor's behavior has mutable state, as is sometimes common in the object-oriented style, make sure you don't share the same `Behavior` instance for multiple `ActorRef`s.  Here are some general ways to avoid the problem:

1. Consider a design without mutable state;
2. Don't leak the `Behavior` instance by only exposing the `ActorRef` instance, for example by only binding the `ActorRef`;
3. If the objective is to only have one single instance of the actor, then make sure that both the `Behavior` and `ActorRef` are singletons, for example by using `@Singleton` or `.asEagerSingleton`;
4. If, instead, there are meant to be multiple instances of the same actor then make sure both `Behavior` and `ActorRef` are named singletons, in Guice by using `@Named` or `.annotatedWith(Names.named(..))`.

### Compile-time dependency injection

Using compile-time dependency injection for Akka Actor Typed requires creating the actor `Behavior` value and using it to spawn the actor:

Scala
: @[compile-time-di](code/scalaguide/akka/typed/fp/AppComponents.scala)

Java
: @[compile-time-di](code/javaguide/akka/typed/oo/AppComponents.java)

### Runtime dependency injection

For runtime dependency injection use the "typed" methods in `AkkaGuiceSupport`, if using the functional-programming style.  For the object-oriented style you must write a `Provider` for your `ActorRef` and bind it.

For instance, given a component in your application or system that needs injecting, like this one:

Scala
: @[main](code/scalaguide/akka/typed/fp/Main.scala)

Java
: @[main](code/javaguide/akka/typed/oo/Main.java)

You can define a Guice `Module` like so:

Scala FP
: @[fp-app-module](code/scalaguide/akka/typed/fp/AppModule.scala)

Scala OO
: @[oo-app-module](code/scalaguide/akka/typed/oo/AppModule.scala)

Java FP
: @[fp-app-module](code/javaguide/akka/typed/fp/AppModule.java)

Java OO
: @[oo-app-module](code/javaguide/akka/typed/oo/AppModule.java)


## Using the `AskPattern` & Typed Scheduler

When [interacting with actors from outside of another Actor](https://doc.akka.io/docs/akka/2.6/typed/interaction-patterns.html#request-response-with-ask-from-outside-an-actor), for example from a `Controller`, you need to use `AskPattern.ask` to send a message to the actor and get a response. The `AskPattern.ask` method requires a `akka.actor.typed.Scheduler` that you can obtain via Dependency Injection.

### Runtime dependency injection

Runtime dependency injection works as any other runtime DI module in Play. The `Scheduler` is part of the default bindings, so the module is enabled automatically, and an instance is available for injection.

### Compile-time dependency injection

If you're using compile-time DI, you can get have access to the `Scheduler` by using the components like below:

Java
: @[scheduler-compile-time-injection](code/javaguide/akka/components/ComponentsWithTypedScheduler.java)

Scala
: @[scheduler-compile-time-injection](code/scalaguide/akka/components/ComponentsWithTypedScheduler.scala)
