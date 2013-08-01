# Accessing an SQL database

## Configuring JDBC connection pools

Play provides a plugin for managing JDBC connection pools. You can configure as many databases you need.

To enable the database plugin add javaJdbc in your build dependencies :

```scala
val appDependencies = Seq(
  javaJdbc
)
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
val appDependencies = Seq(
     // Add your project dependencies here,
     ...
     "mysql" % "mysql-connector-java" % "5.1.18"
     ...
)
```

> **Next:** [[Using Ebean to access your database | JavaEbean]]