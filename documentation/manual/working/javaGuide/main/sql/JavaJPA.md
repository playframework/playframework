<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Integrating with JPA

## Adding dependencies to your project

First you need to tell Play that your project depends on `javaJpa` which will provide JDBC and JPA api dependencies.

There is no built-in JPA implementation in Play; you can choose any available implementation. For example, to use [Hibernate](http://hibernate.org/), just add the following dependency to your project:

@[jpa-sbt-dependencies](code/jpa.sbt)

## Exposing the datasource through JNDI

JPA requires the datasource to be accessible via [JNDI](http://www.oracle.com/technetwork/java/jndi/index.html). You can expose any Play-managed datasource via JNDI by adding this configuration in `conf/application.conf`:

```
db.default.jndiName=DefaultDS
```

See the [[Java Database docs|JavaDatabase]] for more information about how to configure your datasource.

## Creating a Persistence Unit

Next you have to create a proper `persistence.xml` JPA configuration file. Put it into the `conf/META-INF` directory, so it will be properly added to your classpath.

Here is a sample configuration file to use with Hibernate:

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"
             version="2.1">

    <persistence-unit name="defaultPersistenceUnit" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <non-jta-data-source>DefaultDS</non-jta-data-source>
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
        </properties>
    </persistence-unit>

</persistence>
```

Finally you have to tell Play, which persistent unit should be used by your JPA provider. This is done by the `jpa.default` property in your `conf/application.conf`.

```
jpa.default=defaultPersistenceUnit
```

## Deploying Play with JPA

Running Play in development mode while using JPA will work fine, but in order to deploy the application you will need to add this to your `build.sbt` file.

@[jpa-externalize-resources](code/jpa.sbt)

> **Note:** Since Play 2.4 the contents of the `conf` directory are added to the classpath by default. This option will disable that behavior and allow a JPA application to be deployed. The content of conf directory will still be available in the classpath due to it being included in the application's jar file.

## Annotating JPA actions with `@Transactional`

Every JPA call must be done in a transaction. So, to enable JPA for a particular action, annotate it with `@play.db.jpa.Transactional`. This will compose your action method with a JPA `Action` that manages the [Entity Manager](https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html) and the transaction for you. A newly created Entity Manager will then be stored in the current context of each request. To retrieve it from the context arguments Play provides the helper method `play.db.jpa.JPA.em(context)`:

@[jpa-controller-transactional-imports](code/controllers/JPAController.java)

@[jpa-controller-transactional-action](code/controllers/JPAController.java)

If your action runs only queries, you can set the `readOnly` attribute to `true`:

@[jpa-controller-transactional-readonly](code/controllers/JPAController.java)

When using `@Transactional` Play will automatically close and clean up the transaction and the Entity Manager for you after the annotated action method has run. In case an action throws an exception the transaction will be rolled back automatically.
It is also possible to annotate a controller class with `@play.db.jpa.Transactional` so each action within this controller will run inside a JPA transaction.

> Using JPA directly in an Action will limit your ability to use Play asynchronously. Consider arranging your code so that all access to to JPA is wrapped in a custom [[execution context|ThreadPools]], and returns [`java.util.concurrent.CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) to Play.

If you do not annotate your action with `@Transactional` but try to access an Entity Manager using `play.db.jpa.JPA.em(context)`, you will get the following error:

```
java.lang.RuntimeException: No EntityManager found in given Http.Context. Try to annotate your action method with @play.db.jpa.Transactional
```

## Using `play.db.jpa.JPAApi`

Another convenient API Play offers you to work with an [Entity Manager](https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html) and transactions is defined by `play.db.jpa.JPAApi`, which can be injected in other objects like the code below:

@[jpa-controller-api-inject](code/controllers/JPAController.java)

So instead of annotating your action or controller with `@Transactional` you could just use a newly created Entity Manager created by `JPAApi`.

### Examples:

Using `JPAApi.withTransaction(Function<EntityManager, T>)`:

@[jpa-withTransaction-function](code/controllers/JPAController.java)

Using `JPAApi.withTransaction(Consumer<EntityManager>)` to run a batch update:

@[jpa-withTransaction-consumer](code/controllers/JPAController.java)

### Running transactions decoupled from requests

It is likely that you need to run transactions that are not coupled with requests, for instance, transactions executed inside a scheduled job. Luckily by injecting `JPAApi` you are not bound to a request and therefore enables you to do so. This means the above lambda expressions could also be used in any other component by just injecting `JPAApi`.
The following methods are available by `JPAApi` to execute arbitrary code inside a JPA transaction:

* `play.db.jpa.JPAApi.withTransaction(Function<EntityManager, T>)`
* `play.db.jpa.JPAApi.withTransaction(String, Function<EntityManager, T>)`
* `play.db.jpa.JPAApi.withTransaction(String, boolean, Function<EntityManager, T>)`
* `play.db.jpa.JPAApi.withTransaction(Consumer<EntityManager>)`
* `play.db.jpa.JPAApi.withTransaction(String, Consumer<EntityManager>)`
* `play.db.jpa.JPAApi.withTransaction(String, boolean, Consumer<EntityManager>)`

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the setup instructions for using it.
