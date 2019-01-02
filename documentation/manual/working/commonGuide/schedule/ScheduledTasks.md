<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Scheduling asynchronous tasks

You can schedule sending messages to actors and executing tasks (functions or `Runnable` instances). You will get a `Cancellable` back that you can call `cancel` on to cancel the execution of the scheduled operation.

For example, to send a message to the `testActor` every 30 seconds:

Scala
: @[](code/scalaguide/scheduling/MyActorTask.scala)

Java:
: @[](code/javaguide/scheduling/MyActorTask.java)

> **Note:** See [[Scala|ScalaAkka#Dependency-injecting-actors]] or [[Java|JavaAkka#Dependency-injecting-actors]] documentation about how to inject actors.

Similarly, to run a block of code 10 seconds from now, every minute:

Scala
: @[schedule-block-with-interval](code/scalaguide/scheduling/CodeBlockTask.scala)

Java
: @[schedule-block-with-interval](code/javaguide/scheduling/CodeBlockTask.java)

Or to run a block of code once 10 seconds from now:

Scala
: @[schedule-block-once](code/scalaguide/scheduling/CodeBlockTask.scala)

Java
: @[](code/javaguide/scheduling/CodeBlockOnceTask.java)

You can see the Akka documentation to see other possible uses of the scheduler. See the documentation for [`akka.actor.Scheduler` for Scala](https://doc.akka.io/api/akka/2.5/akka/actor/Scheduler.html) or [for Java](https://doc.akka.io/japi/akka/2.5/akka/actor/Scheduler.html).

> **Note**: Instead of using the default `ExecutionContext`, you can instead create a `CustomExecutionContext`. See documentation for [Java](api/java/play/libs/concurrent/CustomExecutionContext.html) or [Scala](api/scala/play/api/libs/concurrent/CustomExecutionContext.html). See the section about it below.

## Starting tasks when your app starts

After defining the tasks as described above, you need to initialize them when your application starts.

### Using Guice Dependency Injection

When using Guice Dependency Injection, you will need to create and enable a module to load the tasks as [eager singletons](https://github.com/google/guice/wiki/Scopes#eager-singletons):

Scala
: @[](code/scalaguide/scheduling/TasksModule.scala)

Java
: @[](code/javaguide/scheduling/TasksModule.java)

And then enable the module in your `application.conf` by adding the following line:

```
play.modules.enabled += "tasks.TasksModule"
```

As the task definitions are completely integrated with the Dependency Injection framework, you can also inject any necessary component inside of them. For more details about how to use Guice Dependency Injection, see [[Scala|ScalaDependencyInjection]] or [[Java|JavaDependencyInjection]] documentation.

### Using compile-time Dependency Injection

When using compile-time Dependency Injection, you just need to start them in your implementation of `BuiltInComponents`:

Scala
: @[](code/scalaguide/scheduling/MyBuiltInComponentsFromContext.scala)

Java
: @[](code/javaguide/scheduling/MyBuiltInComponentsFromContext.java)

This must then be used with your custom `ApplicationLoader` implementation. For more details about how to use compile-time Dependency Injection, see [[Scala|ScalaCompileTimeDependencyInjection]] or [[Java|JavaCompileTimeDependencyInjection]] documentation. 

## Using a `CustomExecutionContext`

You should use a custom execution context when creating tasks that do sync/blocking work. For example, if your task is accessing a database using JDBC, it is doing blocking I/O. If you use the default execution context, your tasks will then block threads that are using to receive and handle requests. To avoid that, you should provide a custom execution context:

Scala
: @[custom-task-execution-context](code/scalaguide/scheduling/TasksCustomExecutionContext.scala)

Java
: @[custom-task-execution-context](code/javaguide/scheduling/TasksCustomExecutionContext.java)


Configure the thread pool as described in [[thread pools documentation|ThreadPools#Using-other-thread-pools]] using `tasks-dispatcher` as the thread pool name, and then inject it in your tasks:

Scala
: @[task-using-custom-execution-context](code/scalaguide/scheduling/TasksCustomExecutionContext.scala)

Java
: @[task-using-custom-execution-context](code/javaguide/scheduling/TasksCustomExecutionContext.java)

## Use third party modules

There are also modules that you can use to schedule tasks. Visit our [[module directory|ModuleDirectory#Task-Schedulers]] page to see a list of available modules.
