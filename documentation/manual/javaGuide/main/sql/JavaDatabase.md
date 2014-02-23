<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Accessing an SQL database

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
db.default.user=playdbuser
db.default.password="a strong password"
```

## How to see SQL Statement in the console?

```properties
db.default.logStatements=true
logger.com.jolbox=DEBUG // for EBean
```

## Accessing the JDBC datasource

The `play.db` package provides access to the configured data sources:

```java
import play.db.*;

DataSource ds = DB.getDatasource();
```

## Obtaining a JDBC connection

You can retrieve a JDBC connection the same way:

```
Connection connection = DB.getConnection();
```
It is important to note that resulting Connections are not automatically disposed at the end of the request cycle. In other words, you are responsible for calling their close() method somewhere in your code so that they can be immediately returned to the pool.

## Exposing the datasource through JNDI

Some libraries expect to retrieve the `Datasource` reference from JNDI. You can expose any Play managed datasource via JDNI by adding this configuration in `conf/application.conf`:

```
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.jndiName=DefaultDS
```

## Importing a Database Driver

Other than for the h2 in-memory database, useful mostly in development mode, Play does not provide any database drivers. Consequently, to deploy in production you will have to add your database driver as an application dependency.

For example, if you use MySQL5, you need to add a [[dependency| SBTDependencies]] for the connector:

```
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.18"
```

> **Next:** [[Using Ebean to access your database | JavaEbean]]
