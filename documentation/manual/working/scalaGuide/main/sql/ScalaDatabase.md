<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
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
db.default.user=playdbuser
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

## Configuring the JDBC Driver

Play is bundled only with an [H2](http://www.h2database.com) database driver. Consequently, to deploy in production you will need to add your database driver as a dependency.

For example, if you use MySQL5, you need to add a [[dependency | SBTDependencies]] for the connector:

```scala
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"
```

Or if the driver can't be found from repositories you can drop the driver into your project's [[unmanaged dependencies|Anatomy]] `lib` directory.

## Accessing the JDBC datasource

The `play.api.db` package provides access to the configured data sources:

```scala
import play.api.db._

val ds = DB.getDataSource()
```

## Obtaining a JDBC connection

There are several ways to retrieve a JDBC connection. The simplest way is:

```scala
val connection = DB.getConnection()
```

Following code show you a JDBC example very simple, working with MySQL 5.*:

```scala
package controllers
import play.api.Play.current
import play.api.mvc._
import play.api.db._

object Application extends Controller {

  def index = Action {
    var outString = "Number is "
    val conn = DB.getConnection()
    try {
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    } finally {
      conn.close()
    }
    Ok(outString)
  }

}
```


But of course you need to call `close()` at some point on the opened connection to return it to the connection pool. Another way is to let Play manage closing the connection for you:

```scala
// access "default" database
DB.withConnection { conn =>
  // do whatever you need with the connection
}
```

For a database other than the default:

```scala
// access "orders" database instead of "default"
DB.withConnection("orders") { conn =>
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

## Selecting and configuring the connection pool

Out of the box, Play provides two database connection pool implementations, [HikariCP](https://github.com/brettwooldridge/HikariCP) and [BoneCP](http://jolbox.com/).  The default is HikariCP, but this can be changed by setting the `play.db.pool` property:

```
play.db.pool=bonecp
```

The full range of configuration options for connection pools can be found by inspecting the `play.db.prototype` property in Play's JDBC [`reference.conf`](resources/confs/play-jdbc/reference.conf).

## Testing

For information on testing with databases, including how to setup in-memory databases and, see [[Testing With Databases|ScalaTestingWithDatabases]].
