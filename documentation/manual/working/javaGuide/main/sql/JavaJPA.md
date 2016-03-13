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

Every JPA call must be done in a transaction so, to enable JPA for a particular action, annotate it with `@play.db.jpa.Transactional`. This will compose your action method with a JPA `Action` that manages the transaction for you:

@[jpa-controller-transactional-imports](code/controllers/JPAController.java)

@[jpa-controller-transactional-action](code/controllers/JPAController.java)

If your action runs only queries, you can set the `readOnly` attribute to `true`:

@[jpa-controller-transactional-readonly](code/controllers/JPAController.java)

## Using `play.db.jpa.JPAApi`

Play offers you a convenient API to work with [Entity Manager](https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html) and Transactions. This API is defined by `play.db.jpa.JPAApi`, which can be injected at other objects like the code below:

@[jpa-controller-api-inject](code/controllers/JPAController.java)

If you already are in a transactional context (because you have annotated your action with `@Transactional`),

@[jpa-access-entity-manager](code/controllers/JPAController.java)

But if you do not annotate your action with `@Transactional` and are trying to access a Entity Manager using `jpaApi.em()`, you will get the following error:

```
java.lang.RuntimeException: No EntityManager found in the context. Try to annotate your action method with @play.db.jpa.Transactional
```

### Running transactions decoupled from requests

It is likely that you need to run transactions that are not coupled with requests, for instance, transactions executed inside a scheduled job. JPAApi has some methods that enable you to do so. The following methods are available to execute arbitrary code inside a JPA transaction:

* `JPAApi.withTransaction(Function<EntityManager, T>)`
* `JPAApi.withTransaction(String, Function<EntityManager, T>)`
* `JPAApi.withTransaction(String, boolean, Function<EntityManager, T>)`
* `JPAApi.withTransaction(Supplier<T>)`
* `JPAApi.withTransaction(Runnable)`
* `JPAApi.withTransaction(String, boolean, Supplier<T>)`

### Examples:

Using `JPAApi.withTransaction(Function<EntityManager, T>)`:

@[jpa-withTransaction-function](code/controllers/JPAController.java)

Using `JPAApi.withTransaction(Runnable)` to run a batch update:

@[jpa-withTransaction-runnable](code/controllers/JPAController.java)

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the setup instructions for using it.
