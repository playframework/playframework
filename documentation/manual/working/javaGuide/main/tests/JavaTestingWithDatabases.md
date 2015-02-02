# Testing with databases

While it is possible to write [[functional tests|JavaFunctionalTest]] that test database access code by starting up a full application including the database, starting up a full application is not often desirable, due to the complexity of having many more components started and running just to test one small part of your application.

Play provides a number of utilities for helping to test database access code that allow it to be tested with a database but in isolation from the rest of your app.  These utilities can easily be used with either ScalaTest or specs2, and can make your database tests much closer to lightweight and fast running unit tests than heavy weight and slow functional tests.

## Using a database

To connect to a database, at a minimum, you just need database driver name and the url of the database, using the [`Database`](api/java/play/db/Database.html) static factory methods.  For example, to connect to MySQL, you might use the following:

@[database](code/javaguide/tests/JavaTestingWithDatabases.java)

This will create a database connection pool for the MySQL `test` database running on `localhost`, with the name `default`.  The name of the database is only used internally by Play, for example, by other features such as evolutions, to load resources associated with that database.

You may want to specify other configuration for the database, including a custom name, or configuration properties such as usernames, passwords and the various connection pool configuration items that Play supports, by supplying a custom name parameter and/or a custom config parameter:

@[full-config](code/javaguide/tests/JavaTestingWithDatabases.java)

After using a database, since the database is typically backed by a connection pool that holds open connections and may also have running threads, you need to shut it down.  This is done by calling the `shutdown` method:

@[shutdown](code/javaguide/tests/JavaTestingWithDatabases.java)

These methods are particularly useful if you use them in combination with JUnit's `@Before` and `@After` annotations, for example:

@[database-junit](code/javaguide/tests/JavaTestingWithDatabases.java)

> **Tip:** You can use this to externalise your test database configuration, using environment variables or system properties to configure what database to use and how to connect to it.  This allows for maximum flexibility for developers to have their own environments set up the way they please, as well as for CI systems that provide particular environments that may differ to development.

### Using an in-memory database

Some people prefer not to require infrastructure such as databases to be installed in order to run tests.  Play provides simple helpers to create an H2 in-memory database for these purposes:

@[in-memory](code/javaguide/tests/JavaTestingWithDatabases.java)

The in-memory database can be configured by supplying a custom name, custom URL arguments, and custom connection pool configuration.  The following shows supplying the `MODE` argument to tell H2 to emulate `MySQL`, as well as configuring the connection pool to log all statements:

@[in-memory-full-config](code/javaguide/tests/JavaTestingWithDatabases.java)

As with the generic database factory, ensure you always shut the in-memory database connection pool down:

@[in-memory-shutdown](code/javaguide/tests/JavaTestingWithDatabases.java)

## Applying evolutions

When running tests, you will typically want your database schema managed for your database.  If you're already using evolutions, it will often make sense to reuse the same evolutions that you use in development and production in your tests.  You may also want to create custom evolutions just for testing.  Play provides some convenient helpers to apply and manage evolutions without having to run a whole Play application.

To apply evolutions, you can use `applyEvolutions` from the [`Evolutions`](api/java/play/db/evolutions/Evolutions.html) static class:

@[apply-evolutions](code/javaguide/tests/JavaTestingWithDatabases.java)

This will load the evolutions from the classpath in the `evolutions/<databasename>` directory, and apply them.

After a test has run, you may want to reset the database to its original state.  If you have implemented your evolutions down scripts in such a way that they will drop all the database tables, you can do this simply by calling the `cleanupEvolutions` method:

@[cleanup-evolutions](code/javaguide/tests/JavaTestingWithDatabases.java)

### Custom evolutions

In some situations you may want to run some custom evolutions in your tests.  Custom evolutions can be used by using a custom [`EvolutionsReader`](api/java/play/db/evolutions/EvolutionsReader.html).  The easiest way to do this is using the static factory methods on `Evolutions`, for example `forDefault` creates an evolutions reader for a simple list of [`Evolution`](api/java/play/db/evolutions/Evolution.html) scripts for the default database.  For example:

@[apply-evolutions-simple](code/javaguide/tests/JavaTestingWithDatabases.java)

Cleaning up custom evolutions is done in the same way as cleaning up regular evolutions, using the `cleanupEvolutions` method:

@[cleanup-evolutions-simple](code/javaguide/tests/JavaTestingWithDatabases.java)

Note though that you don't need to pass the custom evolutions reader here, this is because the state of the evolutions is stored in the database, including the down scripts which will be used to tear down the database.

Sometimes it will be impractical to put your custom evolution scripts in code.  If this is the case, you can put them in the test resources directory, using the `fromClassLoader` factory method:

@[apply-evolutions-custom-path](code/javaguide/tests/JavaTestingWithDatabases.java)

This will load evolutions, in the same structure and format as is done for development and production, from `testdatabase/evolutions/<databasename>/<n>.sql`.

### Integrating with JUnit

Typically you will have many tests that you want to run with the same evolutions, so it will make sense to extract the evolutions setup code into before and after methods, along with the database setup and tear down code.  Here is what a complete test might look like:

@[database-test](code/javaguide/tests/DatabaseTest.java)
