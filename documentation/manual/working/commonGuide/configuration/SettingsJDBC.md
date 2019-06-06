<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring the JDBC pool.

The Play JDBC datasource is managed by [HikariCP](https://brettwooldridge.github.io/HikariCP/).

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

In addition to the classical `driver`, `url`, `username`, `password` configuration properties, it also supports additional tuning parameters if you need them.  The `play.db.prototype` configuration from the Play JDBC `reference.conf` is used as the prototype for the configuration for all database connections.  The defaults for all the available configuration options can be seen here:

@[](/confs/play-jdbc/reference.conf)

When you need to specify some settings for a connection pool, you can override the prototype settings.  For example, to set the default connection pool for BoneCP and set maxConnectionsPerPartition for the default pool, you would set the following in your `application.conf` file:

```
play.db.pool=bonecp
play.db.prototype.bonecp.maxConnectionsPerPartition = 50
```
