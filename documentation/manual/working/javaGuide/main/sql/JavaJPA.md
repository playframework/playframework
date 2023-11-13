<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Using JPA to access your database

> **Important Note:**
> Please be aware that JPA bean validation is currently not supported in Play 2.9. For further information, [[see details further below|JavaJPA#Bean-Validation-Not-Currently-Supported-in-Play-2.9]].

## Adding dependencies to your project

First you need to tell Play that your project depends on `javaJpa` which will provide JDBC and JPA api dependencies.

There is no built-in JPA implementation in Play; you can choose any available implementation. For example, to use [Hibernate](http://hibernate.org/), just add the following dependency to your project:

@[jpa-sbt-dependencies](code/jpa.sbt)

## Exposing the datasource through JNDI

JPA requires the datasource to be accessible via [JNDI](https://www.oracle.com/technetwork/java/jndi/index.html). You can expose any Play-managed datasource via JNDI by adding this configuration in `conf/application.conf`:

```
db.default.jndiName=DefaultDS
```

See the [[Database docs|AccessingAnSQLDatabase]] for more information about how to configure your datasource.

## Creating a Persistence Unit

Next you have to create a proper `persistence.xml` JPA configuration file. Put it into the `conf/META-INF` directory, so it will be properly added to your classpath.

Here is a sample configuration file to use with Hibernate:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">

    <persistence-unit name="defaultPersistenceUnit" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <non-jta-data-source>DefaultDS</non-jta-data-source>
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
        </properties>
    </persistence-unit>

</persistence>
```

Finally you have to tell Play, which persistent unit should be used by your JPA provider. This is done by setting the `jpa.default` property in your `conf/application.conf`.

```
jpa.default=defaultPersistenceUnit
```

## Deploying Play with JPA

Running Play in development mode while using JPA will work fine, but in order to deploy the application you will need to add this to your `build.sbt` file.

@[jpa-externalize-resources](code/jpa.sbt)

> **Note:** More information on how to configure externalized resources can be found [[here|sbtCookbook#Configure-externalized-resources]].
The above settings makes sure the `persistence.xml` file will always stay *inside* the generated application `jar` file.
This is a requirement by the [JPA specification](https://download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf). According to it the `persistence.xml` file has to be in the *same* `jar` file where its persistence-units' entities live, otherwise these entities won't be available for the persistence-units. (You could, however, explicitly add a `jar` file containing entities via `<jar-file>xxx.jar</jar-file>` to a persistence-unit - but that doesn't work well with Play as it would fail with a `FileNotFoundException` in development mode because there is no `jar` file that will be generated in that mode. Further that wouldn't work well in production mode too because when deploying an application, the name of the generated application `jar` file changes with each new release as the current version of the application gets appended to it.)

## Using `play.db.jpa.JPAApi`

Play offers you a convenient API to work with [Entity Manager](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/entitymanager) and Transactions. This API is defined by [`play.db.jpa.JPAApi`](api/java/play/db/jpa/JPAApi.html), which can be injected at other objects like the code below:

@[jpa-repository-api-inject](code/JPARepository.java)

We recommend isolating your JPA operations behind a [Repository](https://martinfowler.com/eaaCatalog/repository.html) or [DAO](https://en.wikipedia.org/wiki/Data_access_object), so that you can manage all your JPA operations with a custom execution context and transactions.  

This means that all JPA operations are done behind the interface -- JPA classes are package private, there is no exposure of persistence aware objects to the rest of the application, and sessions are not held open past the method that defines an asynchronous boundary (i.e. returns `CompletionStage`).  

This may mean that your domain object (aggregate root, in DDD terms) has an internal reference to the repository and calls it to return lists of entities and value objects, rather than holding a session open and using JPA based lazy loading.

## Using a CustomExecutionContext

> **NOTE**: Using JPA directly in an Action -- which uses Play's default rendering thread pool -- will limit your ability to use Play asynchronously because JDBC blocks the thread it's running on. 

You should always use a custom execution context when using JPA, to ensure that Play's rendering thread pool is completely focused on rendering pages and using cores to their full extent.  You can use Play's `CustomExecutionContext` class to configure a custom execution context dedicated to serving JDBC operations.  See [[JavaAsync]] and [[ThreadPools]] for more details.

All the Play example templates on [Play's download page](https://playframework.com/download#examples) that use blocking APIs (i.e. Anorm, JPA) have been updated to use custom execution contexts where appropriate. For example, going to <https://github.com/playframework/play-samples/tree/3.0.x/play-java-jpa-example> shows that the [JPAPersonRepository](https://github.com/playframework/play-samples/blob/3.0.x/play-java-jpa-example/app/models/JPAPersonRepository.java) class takes a `DatabaseExecutionContext` that wraps all the database operations.

For thread pool sizing involving JDBC connection pools, you want a fixed thread pool size matching the connection pool, using a thread pool executor.  Following the advice in [HikariCP's pool sizing page]( https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing), you should configure your JDBC connection pool to double the number of physical cores, plus the number of disk spindles, i.e. if you have a four core CPU and one disk, you have a total of 9 JDBC connections in the pool:

```
# db connections = ((physical_core_count * 2) + effective_spindle_count)
fixedConnectionPool = 9

database.dispatcher {
  executor = "thread-pool-executor"
  throughput = 1
  thread-pool-executor {
    fixed-pool-size = ${fixedConnectionPool}
  }
}
```

### Running JPA transactions

The `JPAApi` provides you various `withTransaction(...)` methods to execute arbitrary code inside a JPA transaction. These methods however do not include a custom execution context and therefore must be wrapped inside a `CompletableFuture` with an IO bound execution context.

### Examples

Using [`JPAApi.withTransaction(Function<EntityManager, T>)`](api/java/play/db/jpa/JPAApi.html#withTransaction\(java.util.function.Function\)):

@[jpa-withTransaction-function](code/JPARepository.java)

Using [`JPAApi.withTransaction(Consumer<EntityManager>)`](api/java/play/db/jpa/JPAApi.html#withTransaction\(java.util.function.Consumer\)) to run a batch update:

@[jpa-withTransaction-consumer](code/JPARepository.java)

## Bean Validation Not Currently Supported in Play 2.9

The [JPA specification defines a validation mode](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a2366) that enables entity validation. If this mode isn't overridden by the user (e.g., `<validation-mode>...</validation-mode>` [in the `persistence.xml`](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#persistence-xml-schema) or via a [property](https://github.com/hibernate/hibernate-orm/blob/6.2.2/hibernate-core/src/main/java/org/hibernate/cfg/AvailableSettings.java#L192)), it defaults to `AUTO`. This means that Hibernate [automatically](https://github.com/hibernate/hibernate-orm/blob/6.2.2/hibernate-core/src/main/java/org/hibernate/integrator/internal/IntegratorServiceImpl.java#L30) tries [to detect](https://github.com/hibernate/hibernate-orm/blob/6.2.2/hibernate-core/src/main/java/org/hibernate/boot/beanvalidation/BeanValidationIntegrator.java#L111-L112C1) the presence of a Bean Validation reference implementation (such as Hibernate Validator) on the classpath and, if found, automatically activates that bean validation. If no implementation is detected, no validation occurs. However, [an exception is thrown](https://github.com/hibernate/hibernate-orm/blob/6.2.2/hibernate-core/src/main/java/org/hibernate/boot/beanvalidation/BeanValidationIntegrator.java#L183-L190) if the mode was _manually_ set to something other than `NONE`.

The challenge arises with Play and Hibernate ORM 6+. In this version, Hibernate ORM no longer detects Hibernate Validator version 6.x (which Play utilizes) on the classpath, as it continues to use `javax.validation`. Hibernate ORM 6+, however, [only detects](https://github.com/hibernate/hibernate-orm/blob/6.2.2/hibernate-core/src/main/java/org/hibernate/boot/beanvalidation/TypeSafeActivator.java#L71-L76) `jakarta.validation` classes.

While Hibernate Validator version 7.x has transitioned to Jakarta, upgrading to that version in Play is currently challenging. Play employs libraries in Play-Java-Forms to assist with binding, but only a newer version of these libraries has adopted Jakarta and supports Hibernate Validator 7. Unfortunately, these newer library versions are only compatible with Java 17. Since Play 2.9 continues to support Java 11, upgrading is not feasible. Attempting to manually override the hibernate-validator version to 7.x will result in a breakage of Play-Java-Form functionality.

If the absence of Bean Validation poses an issue for your use case, please [inform us](https://github.com/playframework/playframework/issues/new). This way, we can explore potential solutions to address this challenge.

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the setup instructions for using it.
