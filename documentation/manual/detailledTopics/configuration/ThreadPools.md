# Understanding Play thread pools

Play framework is, from the bottom up, an asynchronous web framework.  Streams are handled asynchronously using iteratees.  Thread pools are tuned to be low, in comparison to traditional web frameworks, since IO in play-core never blocks.

Because of this, if you plan to write blocking IO code, or code that could potentially do a lot of CPU intensive work, you need to know exactly which thread pool is bearing that workload, and you need to tune it accordingly.  Doing blocking IO without taking this into account is likely to result in very poor performance from Play framework, for example, you may see only a few requests per second being handled, while CPU usage sits at 5%.  In comparison, benchmarks on typical development hardware (eg, a MacBook Pro) have shown Play to be able to handle workloads in the hundreds or even thousands of requests per second without a sweat when tuned correctly.

## Knowing when you are blocking

The most common place that a typical Play application will block is when it's talking to a database.  Unfortunately, none of the major databases provide asynchronous database drivers for the JVM, so for most databases, your only option is to using blocking IO.  A notable exception to this is [ReactiveMongo](http://reactivemongo.org/), a driver for MongoDB that uses Play's Iteratee library to talk to MongoDB.

Other cases when your code may block include:

* Using REST/WebService APIs through a 3rd party client library (ie, not using Play's asynchronous WS API)
* Some messaging technologies only provide synchronous APIs to send messages
* When you open files or sockets directly yourself
* CPU intensive operations that block by virtue of the fact that they take a long time to execute

In general, if the API you are using returns futures, it is non blocking, otherwise it is blocking.

> Note that you may be tempted to therefore wrap your blocking code in Futures.  This does not make it non blocking, it just means the blocking will happen in a different thread.  You still need to make sure that the thread pool that you are using there has enough threads to handle the blocking.

In contrast, the following types of IO do not block:

* The Play WS API
* Asynchronous database drivers such as ReactiveMongo
* Sending/receiving messages to/from Akka actors

## Play's thread pools

Play uses a number of different thread pools for different purposes:

* **Netty boss/worker thread pools** - These are used internally by Netty for handling Netty IO.  An applications code should never be executed by a thread in these thread pools.
* **Iteratee thread pool** - This is used by the iteratees library.  Its size can be configured by setting `iteratee-threadpool-size` in `application.conf`.  It defaults to the number of processors available.
* **Play Internal Thread Pool** - This is used internally by Play.  No application code should ever be executed by a thread in this thread pool, and no blocking should ever be done in this thread pool.  Its size can be configured by setting `internal-threadpool-size` in `application.conf`, and it defaults to the number of available processors.
* **Play default thread pool** - This is the default thread pool in which all application code in Play Framework is executed, excluding some iteratees code.  It is an Akka dispatcher, and can be configured by configuring Akka, described below.  By default, it has one thread per processor.
* **Akka thread pool** - This is used by the Play Akka plugin, and can be configured the same way that you would configure Akka.


## Using the default thread pool

All actions in Play Framework use the default thread pool.  When doing certain asynchronous operations, for example, calling `map` or `flatMap` on a future, you may need to provide an implicit execution context to execute the given functions in.  An execution context is basically another name for a thread pool.

> When using the Java promise API, for most operations you don't get a choice as to which execution context will be used, the Play default execution context will always be used.

In most situations, the appropriate execution context to use will be the Play default thread pool.  This can be used by importing it into your Scala source file:

```scala
import play.api.libs.concurrent.Execution.Implicits._

def someAsyncAction = Action {
  Async {
    WS.get("http://www.example.com").get().map { response =>
      // This code block is executed in the imported default execution context
      // which happens to be the same thread pool in which the outer block of
      // code in this action will be executed.
      Ok("The response code was " + response.status)
    }
  }
}
```

### Configuring the default thread pool

The default thread pool can be configured using standard Akka configuration in `application.conf` under the `play` namespace.  Here is the default configuration:

```
play {
  akka {
    event-handlers = ["akka.event.Logging$DefaultLogger", "akka.event.slf4j.Slf4jEventHandler"]
    loglevel = WARNING
    actor {
      default-dispatcher = {
        fork-join-executor {
          parallelism-factor = 1.0
          parallelism-max = 24
        }
      }
    }
  }
}
```

This configuration instructs Akka to create one thread per available processor, with a maximum of 24 threads in the pool.  The full configuration options available to you can be found [here](http://doc.akka.io/docs/akka/2.1.0/general/configuration.html#Listing_of_the_Reference_Configuration).

> Note that this configuration is separate from the configuration that the Play Akka plugin uses.  The Play Akka plugin is configured separately, by configuring akka in the root namespace (without the play { } surrounding it).

## Using other thread pools

In certain circumstances, you may wish to dispatch work to other thread pools.  This may include CPU heavy work, or IO work, such as database access.  To do this, you should first create a thread pool, this can be done easily in Scala:

```scala
object Contexts {
  implicit val myExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.my-context")
}
```

In this case, we are using Akka to create the execution context, but you could also easily create your own execution contexts using Java executors, or the Scala fork join thread pool, for example.  To configure this Akka execution context, you can add the following configuration to your `application.conf`:

```
akka {
  actor {
    my-context {
      fork-join-executor {
        parallelism-factor = 20.0
        parallelism-max = 200
      }
    }
  }
}
```

To use this excecution context in Scala, you would simply use the scala `Future` companion object function:

```scala
Future {
  // Some blocking or expensive code here
}(Contexts.myExecutionContext)
```

or you could just use it implicitly:

```scala
import Contexts.myExecutionContext

Future {
  // Some blocking or expensive code here
}
```

## Best practices

How you should best divide work in your application between different thread pools greatly depends on the types work that your application is doing, and the control you want to have over how much of which work can be done in parallel.  There is no one size fits all solution to the problem, and the best decision for you will come from understanding the blocking IO requirements of your application and the implications they have on your thread pools.  It may help to do load testing on your application to tune and verify your configuration.

Below we outline a few common profiles that people may want to use in Play Framework:

### Pure asynchronous

In this case, you are doing no blocking IO in your application.  Since you are never blocking, the default configuration of one thread per processor suits your use case perfectly, so no extra configuration needs to be done.  The Play default execution context can be used in all cases.

### Highly synchronous

This profile matches that of a traditional synchronous IO based web framework, such as a Java servlet container.  It uses large thread pools to handle blocking IO.  It is useful for applications where most actions are doing database synchronous IO calls, such as accessing a database, and you don't want or need control over concurrency for different types of work.  This profile is the simplest for handling blocking IO.

In this profile, you would simply use the default execution context everywhere, but configure it to have a very large number of threads in its pool, like so:

```
play {
  akka {
    event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
    loglevel = WARNING
    actor {
      default-dispatcher = {
        fork-join-executor {
          parallelism-min = 300
          parallelism-max = 300
        }
      }
    }
  }
}
```

This profile is recommended for Java applications that do synchronous IO, since it is harder in Java to dispatch work to other threads.

### Many specific thread pools

This profile is for when you want to do a lot of synchronous IO, but you also want to control exactly how much of which types of operations your application does at once.  In this profile, you would only do non blocking operations in the default execution context, and then dispatch blocking operations to different execution contexts for those specific operations.

In this case, you might create a number of different execution contexts for different types of operations, like this:

```scala
object Contexts {
  implicit val simpleDbLookups: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.simple-db-lookups")
  implicit val expensiveDbLookups: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.expensive-db-lookups")
  implicit val dbWriteOperations: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.db-write-operations")
  implicit val expensiveCpuOperations: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.expensive-cpu-operations")
}
```

These might then be configured like so:

```
play {
  akka {
    actor {
      simple-db-lookups {
        fork-join-executor {
          parallelism-factor = 10.0
        }
      }
      expensive-db-lookups {
        fork-join-executor {
          parallelism-max = 4
        }
      }
      db-write-operations {
        fork-join-executor {
          parallelism-factor = 2.0
        }
      }
      expensive-cpu-operations {
        fork-join-executor {
          parallelism-max = 2
        }
      }
    }
  }  
}
```

Then in your code, you would create futures and pass the relevant execution context for the type of work that future was doing.

### Few specific thread pools

This is a combination between the many specific thread pools and the highly synchronized profile.  You would do most simple IO in the default execution context and set the number of threads there to be reasonably high (say 100), but then dispatch certain expensive operations to specific contexts, where you can limit the number of them that are done at one time.
