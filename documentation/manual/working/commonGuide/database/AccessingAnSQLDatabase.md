<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Accessing an SQL database

> **NOTE**: JDBC is a blocking operation that will cause threads to wait.  You can negatively impact the performance of your Play application by running JDBC queries directly in your controller!  Please see the "Configuring a CustomExecutionContext" section.

## Configuring JDBC connection pools

Play provides a plugin for managing JDBC connection pools. You can configure as many databases as you need.

To enable the database plugin add the build dependencies:

Java
: @[jdbc-java-dependencies](code/database.sbt)

Scala
: @[jdbc-scala-dependencies](code/database.sbt)

## Configuring the JDBC Driver dependency

Play does not provide any database drivers. Consequently, to deploy in production you will have to add your database driver as an application dependency.

For example, if you use MySQL5, you need to add a [[dependency| SBTDependencies]] for the connector:

@[jdbc-driver-dependencies](code/database.sbt)

## Databases configuration

Then you must configure a connection pool in the `conf/application.conf` file. By convention, the default JDBC data source must be called `default` and the corresponding configuration properties are `db.default.driver` and `db.default.url`.

```properties
# Default database configuration
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
```

If something isn't properly configured, you will be notified directly in your browser:

[[images/dbError.png]]

You can also change the `default` name by setting `play.db.default`, for example:

```properties
play.db.default = "primary"

db.primary.driver=org.h2.Driver
db.primary.url="jdbc:h2:mem:play"
```

### How to configure several data sources

To configure several data sources:

```properties
# Orders database
db.orders.driver=org.h2.Driver
db.orders.url="jdbc:h2:mem:orders"

# Customers database
db.customers.driver=org.h2.Driver
db.customers.url="jdbc:h2:mem:customers"
```

### H2 database engine connection properties

In memory database:

```properties
# Default database configuration using H2 database engine in an in-memory mode
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
```

File based database:

```properties
# Default database configuration using H2 database engine in a persistent mode
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:/path/to/db-file"
```

The details of the H2 database URLs are found from [H2 Database Engine Cheat Sheet](http://www.h2database.com/html/cheatSheet.html).

### SQLite database engine connection properties

```properties
# Default database configuration using SQLite database engine
db.default.driver=org.sqlite.JDBC
db.default.url="jdbc:sqlite:/path/to/db-file"
```

### PostgreSQL database engine connection properties

```properties
# Default database configuration using PostgreSQL database engine
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://database.example.com/playdb"
```

### MySQL database engine connection properties

```properties
# Default database configuration using MySQL database engine
# Connect to playdb as playdbuser
db.default.driver=com.mysql.jdbc.Driver
db.default.url="jdbc:mysql://localhost/playdb"
db.default.username=playdbuser
db.default.password="a strong password"
```

### Exposing the datasource through JNDI

Some libraries expect to retrieve the `Datasource` reference from [JNDI](https://docs.oracle.com/javase/tutorial/jndi/overview/index.html). You can expose any Play managed datasource via JDNI by adding this configuration in `conf/application.conf`:

```properties
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.jndiName=DefaultDS
```

### How to configure SQL log statement

Not all connection pools offer (out of the box) a way to log SQL statements. HikariCP, per instance, suggests that you use the log capacities of your database vendor. From [HikariCP docs](https://github.com/brettwooldridge/HikariCP/tree/dev#log-statement-text--slow-query-logging):

> **Log Statement Text / Slow Query Logging**
>
> *Like Statement caching, most major database vendors support statement logging through properties of their own driver. This includes Oracle, MySQL, Derby, MSSQL, and others. Some even support slow query logging. We consider this a "development-time" feature. For those few databases that do not support it, jdbcdslog-exp is a good option. Great stuff during development and pre-Production.*

Because of that, Play uses [jdbcdslog-exp](https://github.com/jdbcdslog/jdbcdslog) to enable consistent SQL log statement support for supported pools. The SQL log statement can be configured by database, using `logSql` property:

```properties
# Default database configuration using PostgreSQL database engine
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://database.example.com/playdb"
db.default.logSql=true
```

After that, you can configure the jdbcdslog-exp [log level as explained in their manual](https://code.google.com/p/jdbcdslog/wiki/UserGuide#Setup_logging_engine). Basically, you need to configure your root logger to `INFO` and then decide what jdbcdslog-exp will log (connections, statements and result sets). Here is an example using `logback.xml` to configure the logs:

@[](code/logback-play-logSql.xml)

> **Warning**: Keep in mind that this is intended to be used just in development environments and you should not configure it in production, since there is a performance degradation and it will pollute your logs.

## Accessing the JDBC datasource

Play database packages provides access to the default datasource, primarily through the `Database` (see docs for [Java](api/java/play/db/Database.html) and [Scala](api/scala/play/api/db/Database.html)) class.

Java
: @[java-jdbc-database](code/jdatabase/JavaApplicationDatabase.java)

Scala
: @[scala-jdbc-database](code/sdatabase/ScalaApplicationDatabase.scala)

For a database other than the default:

Java
: @[java-jdbc-named-database](code/jdatabase/JavaNamedDatabase.java)

Scala
: @[scala-jdbc-named-database](code/sdatabase/ScalaNamedDatabase.scala)

In both cases, when using `withConnection`, the connection will be automatically closed at the end of the block.

## Obtaining a JDBC connection

You can retrieve a JDBC connection the same way:

Java
: @[java-jdbc-connection](code/jdatabase/JavaJdbcConnection.java)

Scala
: @[scala-jdbc-connection](code/sdatabase/ScalaJdbcConnection.scala)

It is important to note that resulting Connections are not automatically disposed at the end of the request cycle. In other words, you are responsible for calling their `close()` method somewhere in your code so that they can be immediately returned to the pool.

## Using a `CustomExecutionContext`

You should always use a custom execution context when using JDBC, to ensure that Play's rendering thread pool is completely focused on rendering results and using cores to their full extent.  You can use Play's `CustomExecutionContext` (see docs for [Java](api/java/play/libs/concurrent/CustomExecutionContext.html) and [Scala](api/scala/play/api/libs/concurrent/CustomExecutionContext.html)) class to configure a custom execution context dedicated to serving JDBC operations.  See [[JavaAsync]]/[[ScalaAsync]] and [[ThreadPools]] for more details.

All of the Play example templates on [Play's download page](https://playframework.com/download#examples) that use blocking APIs (i.e. Anorm, JPA) have been updated to use custom execution contexts where appropriate.  For example:

1. Scala: going to [playframework/play-scala-anorm-example/](https://github.com/playframework/play-samples/tree/2.8.x/play-scala-anorm-example) shows that the [CompanyRepository](https://github.com/playframework/play-samples/blob/2.8.x/play-scala-anorm-example/app/models/CompanyRepository.scala) class takes a `DatabaseExecutionContext` that wraps all the database operations.
1. Java: going to [playframework/play-java-jpa-example](https://github.com/playframework/play-samples/tree/2.8.x/play-java-jpa-example/) shows that the [JPAPersonRepository](https://github.com/playframework/play-samples/blob/2.8.x/play-java-jpa-example/app/models/JPAPersonRepository.java) class takes a `DatabaseExecutionContext` that wraps all the database operations.

For thread pool sizing involving JDBC connection pools, you want a fixed thread pool size matching the connection pool, using a thread pool executor.  Following the advice in [HikariCP's pool sizing page](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing), you should configure your JDBC connection pool to double the number of physical cores, plus the number of disk spindles, i.e. if you have a four core CPU and one disk, you have a total of 9 JDBC connections in the pool:

```HOCON
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

## Configuring the connection pool

Out of the box, Play uses [HikariCP](https://github.com/brettwooldridge/HikariCP) as the default database connection pool implementation. Also, you can use your own pool that implements `play.api.db.ConnectionPool` by specifying the fully-qualified class name:

```properties
play.db.pool=your.own.ConnectionPool
```

The full range of configuration options for connection pools can be found by inspecting the `play.db.prototype` property in Play's JDBC [`reference.conf`](resources/confs/play-jdbc/reference.conf).

## Testing

For information on testing with databases, including how to setup in-memory databases and, see :

- [[Java: Testing With Databases|JavaTestingWithDatabases]]
- [[Scala: Testing With Databases|ScalaTestingWithDatabases]]

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the instructions for using it.
