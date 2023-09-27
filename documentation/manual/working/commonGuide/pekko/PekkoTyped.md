<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Integrating with Pekko Typed

Pekko 2.6 marked the new typed Actor API ("Pekko Typed") as stable. The typed API is now officially the main API for Pekko. In the typed API, each actor needs to declares which message type it is able to handle and the type system enforces that only messages of this type can be sent to the actor. Although Play does not fully adopt Pekko Typed, we already provide some APIs to better integrate it in Play applications.

> **Note:** the Pekko classic APIs are still fully supported and existing applications can continue to use them. There are no plans to deprecate or remove Pekko classic API. 

## Pekko Actor Typed styles

Pekko's [Actor Typed API][] has two styles:

1. a ["functional programming" style][fp-style], based on defining an actor `Behavior`s with values; and
2. a ["object-oriented" style][oo-style], based on defining an actor `Behavior`s with subclasses.

[Actor Typed API]: https://pekko.apache.org/docs/pekko/1.0/typed/actors.html
[fp-style]: https://pekko.apache.org/docs/pekko/1.0/typed/actors.html#functional-style
[oo-style]: https://pekko.apache.org/docs/pekko/1.0/typed/actors.html#object-oriented-style

For instance, here's an example of a simple actor that says hello back:

Scala FP
: @[fp-hello-actor](code/scalaguide/pekko/typed/fp/HelloActor.scala)

Scala OO
: @[oo-hello-actor](code/scalaguide/pekko/typed/oo/HelloActor.scala)

Java FP
: @[fp-hello-actor](code/javaguide/pekko/typed/fp/HelloActor.java)

Java OO
: @[oo-hello-actor](code/javaguide/pekko/typed/oo/HelloActor.java)

While here is an example of an actor that depends on Play's `Configuration` in order to return configuration values:

Scala FP
: @[fp-configured-actor](code/scalaguide/pekko/typed/fp/ConfiguredActor.scala)

Scala OO
: @[oo-configured-actor](code/scalaguide/pekko/typed/oo/ConfiguredActor.scala)

Java FP
: @[fp-configured-actor](code/javaguide/pekko/typed/fp/ConfiguredActor.java)

Java OO
: @[oo-configured-actor](code/javaguide/pekko/typed/oo/ConfiguredActor.java)

## Dependency Injection

If your actor's behavior has mutable state, as is sometimes common in the object-oriented style, make sure you don't share the same `Behavior` instance for multiple `ActorRef`s.  Here are some general ways to avoid the problem:

1. Consider a design without mutable state;
2. Don't leak the `Behavior` instance by only exposing the `ActorRef` instance, for example by only binding the `ActorRef`;
3. If the objective is to only have one single instance of the actor, then make sure that both the `Behavior` and `ActorRef` are singletons, for example by using `@Singleton` or `.asEagerSingleton`;
4. If, instead, there are meant to be multiple instances of the same actor then make sure both `Behavior` and `ActorRef` are named singletons, in Guice by using `@Named` or `.annotatedWith(Names.named(..))`.

### Compile-time dependency injection

Using compile-time dependency injection for Pekko Actor Typed requires creating the actor `Behavior` value and using it to spawn the actor:

Scala
: @[compile-time-di](code/scalaguide/pekko/typed/fp/AppComponents.scala)

Java
: @[compile-time-di](code/javaguide/pekko/typed/oo/AppComponents.java)

### Runtime dependency injection

For runtime dependency injection use the "typed" methods in `PekkoGuiceSupport`, if using the functional-programming style.  For the object-oriented style you must write a `Provider` for your `ActorRef` and bind it.

For instance, given a component in your application or system that needs injecting, like this one:

Scala
: @[main](code/scalaguide/pekko/typed/fp/Main.scala)

Java
: @[main](code/javaguide/pekko/typed/oo/Main.java)

You can define a Guice `Module` like so:

Scala FP
: @[fp-app-module](code/scalaguide/pekko/typed/fp/AppModule.scala)

Scala OO
: @[oo-app-module](code/scalaguide/pekko/typed/oo/AppModule.scala)

Java FP
: @[fp-app-module](code/javaguide/pekko/typed/fp/AppModule.java)

Java OO
: @[oo-app-module](code/javaguide/pekko/typed/oo/AppModule.java)


## Using the `AskPattern` & Typed Scheduler

When [interacting with actors from outside of another Actor](https://pekko.apache.org/docs/pekko/1.0/typed/interaction-patterns.html#request-response-with-ask-from-outside-an-actor), for example from a `Controller`, you need to use `AskPattern.ask` to send a message to the actor and get a response. The `AskPattern.ask` method requires a `pekko.actor.typed.Scheduler` that you can obtain via Dependency Injection.

### Runtime dependency injection

Runtime dependency injection works as any other runtime DI module in Play. The `Scheduler` is part of the default bindings, so the module is enabled automatically, and an instance is available for injection.

### Compile-time dependency injection

If you're using compile-time DI, you can get have access to the `Scheduler` by using the components like below:

Java
: @[scheduler-compile-time-injection](code/javaguide/pekko/components/ComponentsWithTypedScheduler.java)

Scala
: @[scheduler-compile-time-injection](code/scalaguide/pekko/components/ComponentsWithTypedScheduler.scala)
