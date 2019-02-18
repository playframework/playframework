<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Accessing an SQL database

> **NOTE**: JDBC is a blocking operation that will cause threads to wait.  You can negatively impact the performance of your Play application by running JDBC queries directly in your controller!  Please see the "Configuring a CustomExecutionContext" section.

## Configuring JDBC connection pools

Play provides a plugin for managing JDBC connection pools. You can configure as many databases as you need.

To enable the database plugin add javaJdbc in your build dependencies :

```scala
libraryDependencies += javaJdbc
```

Then you must configure a connection pool in the `conf/application.conf` file. By convention the default JDBC data source must be called `default`:

```properties
# Default database configuration
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
```

To configure several data sources:

```properties
# Orders database
db.orders.driver=org.h2.Driver
db.orders.url="jdbc:h2:mem:orders"

# Customers database
db.customers.driver=org.h2.Driver
db.customers.url="jdbc:h2:mem:customers"
```

If something isn’t properly configured, you will be notified directly in your browser:

[[images/dbError.png]]

### H2 database engine connection properties

```properties
# Default database configuration using H2 database engine in an in-memory mode
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
```

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

## Accessing the JDBC datasource

The `play.db` package provides access to the default datasource, primarily through the [`play.db.Database`](api/java/play/db/Database.html) class.

@[](code/JavaApplicationDatabase.java)

For a database other than the default:

@[](code/JavaNamedDatabase.java)

## Configuring a CustomExecutionContext

You should always use a custom execution context when using JDBC, to ensure that Play's rendering thread pool is completely focused on rendering pages and using cores to their full extent.  You can use Play's [`CustomExecutionContext`](api/java/play/libs/concurrent/CustomExecutionContext.html) class to configure a custom execution context dedicated to serving JDBC operations.  See [[JavaAsync]] and [[ThreadPools]] for more details.

All of the Play example templates on [Play's download page](https://playframework.com/download#examples) that use blocking APIs (i.e. Anorm, JPA) have been updated to use custom execution contexts where appropriate.  For example, going to https://github.com/playframework/play-java-jpa-example/ shows that the [JPAPersonRepository](https://github.com/playframework/play-java-jpa-example/blob/2.6.x/app/models/JPAPersonRepository.java) class takes a `DatabaseExecutionContext` that wraps all the database operations.

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

## Obtaining a JDBC connection

You can retrieve a JDBC connection the same way:

@[](code/JavaJdbcConnection.java)

It is important to note that resulting Connections are not automatically disposed at the end of the request cycle. In other words, you are responsible for calling their close() method somewhere in your code so that they can be immediately returned to the pool.

## Exposing the datasource through JNDI

Some libraries expect to retrieve the `Datasource` reference from JNDI. You can expose any Play managed datasource via JDNI by adding this configuration in `conf/application.conf`:

```
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.jndiName=DefaultDS
```

## How to configure SQL log statement

Not all connection pools offer (out of the box) a way to log SQL statements. HikariCP, per instance, suggests that you use the log capacities of your database vendor. From [HikariCP docs](https://github.com/brettwooldridge/HikariCP/tree/dev#log-statement-text--slow-query-logging):

#### *Log Statement Text / Slow Query Logging*

*Like Statement caching, most major database vendors support statement logging through properties of their own driver. This includes Oracle, MySQL, Derby, MSSQL, and others. Some even support slow query logging. We consider this a "development-time" feature. For those few databases that do not support it, jdbcdslog-exp is a good option. Great stuff during development and pre-Production.*

Because of that, Play uses [jdbcdslog-exp](https://github.com/jdbcdslog/jdbcdslog) to enable consistent SQL log statement support for supported pools. The SQL log statement can be configured by database, using `logSql` property:

```properties
# Default database configuration using PostgreSQL database engine
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://database.example.com/playdb"
db.default.logSql=true
```

After that, you can configure the jdbcdslog-exp [log level as explained in their manual](https://code.google.com/p/jdbcdslog/wiki/UserGuide#Setup_logging_engine). Basically, you need to configure your root logger to `INFO` and then decide what jdbcdslog-exp will log (connections, statements and result sets). Here is an example using `logback.xml` to configure the logs:

@[](/confs/play-logback/logback-play-logSql.xml)

> **Warning**: Keep in mind that this is intended to be used just in development environments and you should not configure it in production, since there is a performance degradation and it will pollute your logs.

## Configuring the JDBC Driver dependency

Play does not provide any database drivers. Consequently, to deploy in production you will have to add your database driver as an application dependency.

For example, if you use MySQL5, you need to add a [[dependency| sbtDependencies]] for the connector:

```
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"
```

## Selecting and configuring the connection pool

Out of the box, Play provides two database connection pool implementations, [HikariCP](https://github.com/brettwooldridge/HikariCP) and [BoneCP](http://www.jolbox.com/).  The default is HikariCP, but this can be changed by setting the `play.db.pool` property:

```
play.db.pool=bonecp
```

The full range of configuration options for connection pools can be found by inspecting the `play.db.prototype` property in Play's JDBC [`reference.conf`](resources/confs/play-jdbc/reference.conf).

## Testing

For information on testing with databases, including how to setup in-memory databases and, see [[Testing With Databases|JavaTestingWithDatabases]].

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the instructions for using it.
