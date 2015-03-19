<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring the JDBC pool.

The Play JDBC datasource is managed by [HikariCP](http://brettwooldridge.github.io/HikariCP/). 

## Special URLs

Play supports special url format for both **MySQL** and **PostgreSQL**:

```
# To configure MySQL
db.default.url="mysql://user:password@localhost/database"

# To configure PostgreSQL
db.default.url="postgres://user:password@localhost/database"
```

A non-standard port of the database service can be specified:

```
# To configure MySQL running in Docker
db.default.url="mysql://user:password@localhost:port/database"

# To configure PostgreSQL running in Docker
db.default.url="postgres://user:password@localhost:port/database"
```


## Reference

In addition to the classical `driver`, `url`, `user`, `password` configuration properties, it also supports additional tuning parameters if you need them:

```properties
# The JDBC driver to use
db.default.driver=org.h2.Driver

# The JDBC url
db.default.url="jdbc:h2:mem:play"

# User name
db.default.username=sa

# Password
db.default.password=secret

# Set a connection's default autocommit setting
db.default.autoCommit=true

# Set a connection's default isolation level
db.default.transactionIsolation=TRANSACTION_READ_COMMITTED

# The number of connections to create per pool.
db.default.maximumPoolSize=15

# This property controls the minimum number of idle connections 
# that HikariCP tries to maintain in the pool.
db.default.minimumIdle=5

# How long to wait before attempting to obtain a 
# connection again after a failure.
db.default.acquireRetryDelay=5 seconds

# The maximum time to wait before a call 
# to getConnection is timed out.
db.default.connectionTimeout=1 second

# This property controls the maximum amount of time that a 
# connection is allowed to sit idle in the pool.
db.default.idleTimeout=10 minute

# This property controls the maximum lifetime of a connection in 
# the pool. When a connection reaches this timeout it will be 
# retired from the pool, subject to a maximum variation of +30 seconds.
db.default.maxLifetime=5 minutes

# An initial SQL statement that is run only when 
# a connection is first created.
db.default.connectionInitSql="SELECT 1"
```

For a complete list of what can be configured, please see the [HikariCP documentation](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby). Also, see if your database implementation of `java.sql.DataSource` offers more advanced configuration. A very complete list of database specific DataSource can also be found in [HikariCP documentation](https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names).