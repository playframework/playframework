<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Testing your application

Test source files must be placed in your application’s `test` folder. You can run tests from the Play console using the `test` (run all tests) and `test-only` (run one test class: `test-only my.namespace.MySpec`) tasks.

## Using JUnit

The default way to test a Play application is with [JUnit](http://www.junit.org/).

@[simple](code/javaguide/tests/SimpleTest.java)

> **Note:** A new process is forked each time `test` or `test-only` is run.  The new process uses default JVM settings.  Custom settings can be added to `build.sbt`.  For example:

> ```scala
> javaOptions in Test ++= Seq(
>   "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9998",
>   "-Xms512M",
>   "-Xmx1536M",
>   "-Xss1M",
>   "-XX:MaxPermSize=384M"
> )
> ```

## Running in a fake application

If the code you want to test depends on a running application, you can easily create a `FakeApplication` on the fly:

Java
: @[fakeapp](code/javaguide/tests/JavaTest.java)

Java 8
: @[fakeapp](java8code/java8guide/tests/JavaTest.java)

You can also pass (or override) additional application configuration, or mock any plugin. For example to create a `FakeApplication` using a `default` in-memory database:

```java
fakeApplication(inMemoryDatabase())
```

> **Note:** Applications using Ebean ORM may be written to rely on Play's automatic getter/setter generation.  Play also rewrites field accesses to use the generated getters/setters.  Ebean relies on calls to the setters to do dirty checking.  In order to use these patterns in JUnit tests, you will need to enable Play's field access rewriting in test by adding the following to `build.sbt`:

> ```scala
> compile in Test <<= PostCompile(Test)
> ```  

> **Next:** [[Writing functional tests | JavaFunctionalTest]]
