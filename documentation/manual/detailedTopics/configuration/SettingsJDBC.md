<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring the JDBC pool.

The Play JDBC datasource is managed by [BoneCP](http://jolbox.com/). 

## Special URLs

Play supports special url format for both **MySQL** and **PostgreSQL**:

```
# To configure MySQL
db.default.url="mysql://user:password@localhost/database"

# To configure PostgreSQL
db.default.url="postgres://user:password@localhost/database"
```

## Reference

In addition to the classical `driver`, `url`, `user`, `password` configuration properties, it also supports additional tuning parameters if you need them:

```properties
# The JDBC driver to use
db.default.driver=org.h2.Driver

# The JDBC url
db.default.url="jdbc:h2:mem:play"

# User name
db.default.user=sa

# Password
db.default.password=secret

# Set a connection's default autocommit setting
db.default.autocommit=true

# Set a connection's default isolation level
db.default.isolation=READ_COMMITTED

# In order to reduce lock contention and thus improve performance, 
# each incoming connection request picks off a connection from a 
# pool that has thread-affinity. 
# The higher this number, the better your performance will be for the 
# case when you have plenty of short-lived threads. 
# Beyond a certain threshold, maintenance of these pools will start 
# to have a negative effect on performance (and only for the case 
# when connections on a partition start running out).
db.default.partitionCount=2

# The number of connections to create per partition. Setting this to 
# 5 with 3 partitions means you will have 15 unique connections to the 
# database. Note that BoneCP will not create all these connections in 
# one go but rather start off with minConnectionsPerPartition and 
# gradually increase connections as required.
db.default.maxConnectionsPerPartition=5

# The number of initial connections, per partition.
db.default.minConnectionsPerPartition=5

# When the available connections are about to run out, BoneCP will 
# dynamically create new ones in batches. This property controls 
# how many new connections to create in one go (up to a maximum of 
# maxConnectionsPerPartition). Note: This is a per-partition setting.
db.default.acquireIncrement=1

# After attempting to acquire a connection and failing, try to 
# connect this number of times before giving up.
db.default.acquireRetryAttempts=10

# How long to wait before attempting to obtain a 
# connection again after a failure.
db.default.acquireRetryDelay=5 seconds

# The maximum time to wait before a call 
# to getConnection is timed out.
db.default.connectionTimeout=1 second

# Idle max age
db.default.idleMaxAge=10 minute

# This sets the time for a connection to remain idle before sending a test query to the DB. 
# This is useful to prevent a DB from timing out connections on its end. 
db.default.idleConnectionTestPeriod=5 minutes

# An initial SQL statement that is run only when 
# a connection is first created.
db.default.initSQL="SELECT 1"

# If enabled, log SQL statements being executed.
db.default.logStatements=false

# The maximum connection age.
db.default.maxConnectionAge=1 hour

# The maximum query execution time. Queries slower than this will be logged as a warning.
db.queryExecuteTimeLimit=1 second
```

> **Next:** [[Configuring Play's thread pools|ThreadPools]]
