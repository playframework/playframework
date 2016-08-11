# JPA Migration

## Removed Deprecated Methods

The following deprecated methods have been removed in Play 2.6.

* `play.db.jpa.JPA.jpaApi`
* `play.db.jpa.JPA.em(key)`
* `play.db.jpa.JPA.bindForAsync(em)`
* `play.db.jpa.JPA.withTransaction`

Please use a `JPAApi` injected instance, or create a `JPAApi` instance with `JPA.createFor`.

## Added Async Warning

Added the following to [[JavaJPA]]:

> Using JPA directly in an Action will limit your ability to use Play asynchronously.  Consider arranging your code so that all access to to JPA is wrapped in a custom [[execution context|ThreadPools]], and returns [`java.util.concurrent.CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) to Play.
