<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Using JPA to access your database

## Adding dependencies to your project

First you need to tell Play that your project depends on `javaJpa` which provides JDBC and Jakarta Persistence API dependencies. Play keeps Jakarta Persistence 3.2 as its minimum API dependency and supports Hibernate ORM 7 with API 3.2 and Hibernate ORM 8 with API 4.0. Hibernate ORM 8 is the recommended provider.

There is no built-in JPA implementation in Play; you can choose any available implementation. For example, to use [Hibernate](http://hibernate.org/), just add the following dependency to your project:

@[jpa-sbt-dependencies](code/jpa.sbt)

Hibernate ORM 8 requires Jakarta Persistence 4.0 on both the compile and runtime classpaths. The dependency above brings in that API transitively in ordinary sbt and Gradle builds. If you declare Hibernate only at runtime, or consume `javaJpa` using Maven, add the Jakarta Persistence API version required by Hibernate explicitly as a compile dependency.

When using Hibernate ORM 7 or newer, keep these changes in mind:

- Prefer listing your entities explicitly with `<class>...</class>` entries in `persistence.xml`. Automatic classpath scanning is no longer the old default. If you prefer automatic discovery, add Hibernate's `hibernate-scan-jandex` module. With Hibernate ORM 8, also set `<exclude-unlisted-classes>false</exclude-unlisted-classes>` to scan the persistence-unit root. See the Hibernate ORM 7 migration guide section on [Jandex scanning](https://docs.hibernate.org/orm/7.0/migration-guide/#jandex-scanning) and the [Hibernate ORM 8 migration guide](https://docs.hibernate.org/orm/8.0/migration-guide/).
- If you do not use Bean Validation for JPA, set `<validation-mode>NONE</validation-mode>` in `persistence.xml`. With Hibernate ORM 8, also set `hibernate.tooling.schema.apply_validation_constraints` to `DISABLED` under `<properties>`, as shown below. The two settings control lifecycle validation and [validation-derived DDL constraints](https://docs.hibernate.org/orm/8.0/migration-guide/#validation-ddl-influence) separately.

## Using Hibernate ORM 7

To use Hibernate ORM 7, replace the Hibernate dependency above with a 7.x version and keep Jakarta Persistence API 3.2. No override of Play's API dependency is needed. Use `version="3.2"` and `persistence_3_2.xsd` in `persistence.xml`; the Jakarta Persistence namespace and explicit entity registration shown below are unchanged.

Hibernate ORM 7 propagates validator bootstrap failures in `AUTO` validation mode. If you do not use Bean Validation for JPA, `<validation-mode>NONE</validation-mode>` alone is sufficient to skip that integration with Hibernate ORM 7. See the [Hibernate ORM 7 migration notes on validator integration](https://docs.hibernate.org/orm/7.0/migration-guide/#validator-integration-auto).

The transaction examples in this guide use Jakarta Persistence 4.0 APIs with Hibernate ORM 8. With Hibernate ORM 7, keep application code compatible with API 3.2: use `Query.executeUpdate()` for native updates instead of `createNativeStatement(...).execute()`, and do not use `Statement`, `@NamedStatement`, or `@NamedNativeStatement`. Play's integration supports both providers, but application code and libraries using changed API signatures must be compiled against the appropriate API version.

## Exposing the datasource through JNDI

JPA requires the datasource to be accessible via [JNDI](https://www.oracle.com/technetwork/java/jndi/index.html). You can expose any Play-managed datasource via JNDI by adding this configuration in `conf/application.conf`:

```
db.default.jndiName=DefaultDS
```

See the [[Database docs|AccessingAnSQLDatabase]] for more information about how to configure your datasource.

## Creating a Persistence Unit

Next you have to create a proper `persistence.xml` JPA configuration file. Put it into the `conf/META-INF` directory, so it will be properly added to your classpath.

Here is a sample configuration file to use with Hibernate ORM 8:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_4_0.xsd"
             version="4.0">

    <persistence-unit name="defaultPersistenceUnit" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <non-jta-data-source>DefaultDS</non-jta-data-source>
        <class>models.MyEntity</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>
        <validation-mode>NONE</validation-mode>
        <properties>
            <property name="hibernate.tooling.schema.apply_validation_constraints" value="DISABLED"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
        </properties>
    </persistence-unit>

</persistence>
```

The Jakarta Persistence namespace and the Hibernate provider class are unchanged. The example uses the 4.0 schema and lists its entities explicitly, before `<exclude-unlisted-classes>` and `<validation-mode>` as required by the schema. Hibernate ORM 8 can also read older persistence descriptors. If you register package or module annotations, use `<package-descriptor>com.example</package-descriptor>` or `<module-descriptor>com.example.module</module-descriptor>` instead of listing `package-info` or `module-info` as a `<class>`.

These validation settings disable both lifecycle callbacks and validation-derived DDL constraints. In Hibernate ORM 8, the DDL setting defaults to `AUTO`, so `<validation-mode>NONE</validation-mode>` alone can still bootstrap Hibernate Validator. The `PlayJava` sbt plugin includes Hibernate Validator through `javaForms`, without a Jakarta Expression Language implementation by default, so JPA startup fails with those dependencies unless the DDL opt-out is also set.

To enable lifecycle validation, use `AUTO` or `CALLBACK` for `<validation-mode>`. To apply validation constraints to DDL, use `AUTO` or `REQUIRED` for `hibernate.tooling.schema.apply_validation_constraints`. When using Hibernate Validator, also add a Jakarta Expression Language implementation to your classpath, or configure a non-EL message interpolator.

For applications upgrading from an earlier Play version, see the [[JPA migration notes|Migration31#JPA-supports-Hibernate-ORM-8-and-7]].

Finally you have to tell Play, which persistent unit should be used by your JPA provider. This is done by setting the `jpa.default` property in your `conf/application.conf`.

```
jpa.default=defaultPersistenceUnit
```

## Deploying Play with JPA

Running Play in development mode while using JPA will work fine, but in order to deploy the application you will need to add this to your `build.sbt` file.

@[jpa-externalize-resources](code/jpa.sbt)

> **Note:** More information on how to configure externalized resources can be found [[here|sbtCookbook#Configure-externalized-resources]].
The above settings makes sure the `persistence.xml` file will always stay *inside* the generated application `jar` file.
This is a requirement by the [Jakarta Persistence specification](https://jakarta.ee/specifications/persistence/4.0/). According to it the `persistence.xml` file has to be in the *same* `jar` file where its persistence-units' entities live, otherwise these entities won't be available for the persistence-units. (You could, however, explicitly add a `jar` file containing entities via `<jar-file>xxx.jar</jar-file>` to a persistence-unit - but that doesn't work well with Play as it would fail with a `FileNotFoundException` in development mode because there is no `jar` file that will be generated in that mode. Further that wouldn't work well in production mode too because when deploying an application, the name of the generated application `jar` file changes with each new release as the current version of the application gets appended to it.)

## Using `play.db.jpa.JPAApi`

Play offers you a convenient API to work with [Entity Manager](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/EntityManager.html) and Transactions. This API is defined by [`play.db.jpa.JPAApi`](api/java/play/db/jpa/JPAApi.html), which can be injected at other objects like the code below:

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

These examples target Hibernate ORM 8 and Jakarta Persistence 4.0. See [[Using Hibernate ORM 7|JavaJPA#Using-Hibernate-ORM-7]] if you use the older provider.

Using [`JPAApi.withTransaction(Function<EntityManager, T>)`](api/java/play/db/jpa/JPAApi.html#withTransaction\(java.util.function.Function\)):

@[jpa-withTransaction-function](code/JPARepository.java)

Using [`JPAApi.withTransaction(Consumer<EntityManager>)`](api/java/play/db/jpa/JPAApi.html#withTransaction\(java.util.function.Consumer\)) to run a batch update:

@[jpa-withTransaction-consumer](code/JPARepository.java)

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the setup instructions for using it.
