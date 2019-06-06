<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# JPA Migration

## Removed Deprecated Methods

The following deprecated methods have been removed in Play 2.6.

* `play.db.jpa.JPA.jpaApi`
* `play.db.jpa.JPA.em(key)`
* `play.db.jpa.JPA.bindForAsync(em)`
* `play.db.jpa.JPA.withTransaction`

Please use a `JPAApi` injected instance as specified in [[Using play.db.jpa.JPAApi|JavaJPA#Using-play.db.jpa.JPAApi]].

## Deprecated JPA Class

As of 2.6.1, the `play.db.jpa.JPA` class has been deprecated, as it uses global state under the hood.  The deprecation was mistakenly left out of 2.6.0.

Please use a `JPAApi` injected instance as specified in [[Using play.db.jpa.JPAApi|JavaJPA#Using-play.db.jpa.JPAApi]].

## Added Async Warning

Added the following to [[JavaJPA]]:

> Using JPA directly in an Action will limit your ability to use Play asynchronously.  Consider arranging your code so that all access to to JPA is wrapped in a custom [[execution context|ThreadPools]], and returns [`java.util.concurrent.CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) to Play.
