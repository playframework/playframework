<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Accessing an SQL database

## Configuring JDBC connection pools

Play provides a plug-in for managing JDBC connection pools. You can configure as many databases as you need.

To enable the database plug-in, add jdbc in your build dependencies :

```scala
libraryDependencies += jdbc
```

Then you must configure a connection pool in the `conf/application.conf` file. By convention, the default JDBC data source must be called `default` and the corresponding configuration properties are `db.default.driver` and `db.default.url`.

If something isn’t properly configured you will be notified directly in your browser:

[[images/dbError.png]]

> **Note:** You likely need to enclose the JDBC URL configuration value with double quotes, since ':' is a reserved character in the configuration syntax.

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

## How to configure several data sources

```properties
# Orders database
db.orders.driver=org.h2.Driver
db.orders.url="jdbc:h2:mem:orders"

# Customers database
db.customers.driver=org.h2.Driver
db.customers.url="jdbc:h2:mem:customers"
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

Play is bundled only with an [H2](http://www.h2database.com) database driver. Consequently, to deploy in production you will need to add your database driver as a dependency.

For example, if you use MySQL5, you need to add a [[dependency|SBTDependencies]] for the connector:

```scala
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.36"
```

Or if the driver can't be found from repositories you can drop the driver into your project's [[unmanaged dependencies|Anatomy]] `lib` directory.

## Accessing the JDBC datasource

The `play.api.db` package provides access to the configured data sources:

```scala
import play.api.db._

val ds = DB.getDataSource()
```

## Obtaining a JDBC connection

There are several ways to retrieve a JDBC connection. The following code show you a JDBC example very simple, working with MySQL 5.*:

@[](code/ScalaControllerInject.scala)

But of course you need to call `close()` at some point on the opened connection to return it to the connection pool. Another way is to let Play manage closing the connection for you:

```scala
// access "default" database
db.withConnection { conn =>
  // do whatever you need with the connection
}
```

The connection will be automatically closed at the end of the block.

> **Tip:** Each `Statement` and `ResultSet` created with this connection will be closed as well.

A variant is to set the connection's auto-commit to `false` and to manage a transaction for the block:

```scala
DB.withTransaction { conn =>
  // do whatever you need with the connection
}
```

For a database other than the default:

@[](code/ScalaInjectNamed.scala)

## Selecting and configuring the connection pool

Out of the box, Play provides two database connection pool implementations, [HikariCP](https://github.com/brettwooldridge/HikariCP) and [BoneCP](http://www.jolbox.com/). **The default is HikariCP**, but this can be changed by setting the `play.db.pool` property:

```
play.db.pool=bonecp
```

The full range of configuration options for connection pools can be found by inspecting the `play.db.prototype` property in Play's JDBC [`reference.conf`](resources/confs/play-jdbc/reference.conf).

## Testing

For information on testing with databases, including how to setup in-memory databases and, see [[Testing With Databases|ScalaTestingWithDatabases]].

## Enabling Play database evolutions

Read [[Evolutions]] to find out what Play database evolutions are useful for, and follow the setup instructions for using it.
