<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# The Logging API

Using logging in your application can be useful for monitoring, debugging, error tracking, and business intelligence. Play provides an API for logging which is accessed through the [`Logger`](api/scala/play/api/Logger$.html) object and uses [Logback](https://logback.qos.ch/) as the default logging engine.

## Logging architecture

The logging API uses a set of components that help you to implement an effective logging strategy.

#### Logger
Your application can define [`Logger`](api/scala/play/api/Logger.html) instances to send log message requests. Each `Logger` has a name which will appear in log messages and is used for configuration.  The Logger API is based on SLF4J, and so `Logger` is based on the `org.slf4j.Logger` interface.

Loggers follow a hierarchical inheritance structure based on their naming. A logger is said to be an ancestor of another logger if its name followed by a dot is the prefix of descendant logger name. For example, a logger named "com.foo" is the ancestor of a logger named "com.foo.bar.Baz." All loggers inherit from a root logger. Logger inheritance allows you to configure a set of loggers by configuring a common ancestor.

Play applications are provided a default logger named "application" or you can create your own loggers. The Play libraries use a logger named "play", and some third party libraries will have loggers with their own names.

#### Log levels
Log levels are used to classify the severity of log messages. When you write a log request statement you will specify the severity and this will appear in generated log messages.

This is the set of available log levels, in decreasing order of severity.

- `OFF` - Used to turn off logging, not as a message classification.
- `ERROR` - Runtime errors, or unexpected conditions.
- `WARN` - Use of deprecated APIs, poor use of API, 'almost' errors, other runtime situations that are undesirable or unexpected, but not necessarily "wrong".
- `INFO` - Interesting runtime events such as application startup and shutdown.
- `DEBUG` - Detailed information on the flow through the system.
- `TRACE` - Most detailed information.

In addition to classifying messages, log levels are used to configure severity thresholds on loggers and appenders. For example, a logger set to level `INFO` will log any request of level `INFO` or higher (`INFO`, `WARN`, `ERROR`) but will ignore requests of lower severities (`DEBUG`, `TRACE`). Using `OFF` will ignore all log requests.

#### Appenders
The logging API allows logging requests to print to one or many output destinations called "appenders." Appenders are specified in configuration and options exist for the console, files, databases, and other outputs.

Appenders combined with loggers can help you route and filter log messages. For example, you could use one appender for a logger that logs useful data for analytics and another appender for errors that is monitored by an operations team.

> Note: For further information on architecture, see the [Logback documentation](https://logback.qos.ch/manual/architecture.html).

## Using Loggers
First import the `Logger` class and companion object:

@[logging-import](code/ScalaLoggingSpec.scala)

### The default Logger

The `Logger` object is your default logger and uses the name "application." You can use it to write log request statements:

@[logging-default-logger](code/ScalaLoggingSpec.scala)

Using Play's default logging configuration, these statements will produce console output similar to this:

```text
[debug] application - Attempting risky calculation.
[error] application - Exception with riskyCalculation
java.lang.ArithmeticException: / by zero
    at controllers.Application$.controllers$Application$$riskyCalculation(Application.scala:32) ~[classes/:na]
    at controllers.Application$$anonfun$test$1.apply(Application.scala:18) [classes/:na]
    at controllers.Application$$anonfun$test$1.apply(Application.scala:12) [classes/:na]
    at play.api.mvc.ActionBuilder$$anonfun$apply$17.apply(Action.scala:390) [play_2.10-2.3-M1.jar:2.3-M1]
    at play.api.mvc.ActionBuilder$$anonfun$apply$17.apply(Action.scala:390) [play_2.10-2.3-M1.jar:2.3-M1]
```

Note that the messages have the log level, logger name, message, and stack trace if a Throwable was used in the log request.

### Creating your own loggers

Although it may be tempting to use the default logger everywhere, it's generally a bad design practice. Creating your own loggers with distinct names allows for flexible configuration, filtering of log output, and pinpointing the source of log messages.

You can create a new logger using the `Logger.apply` factory method with a name argument:

@[logging-create-logger-name](code/ScalaLoggingSpec.scala)

A common strategy for logging application events is to use a distinct logger per class using the class name. The logging API supports this with a factory method that takes a class argument:

@[logging-create-logger-class](code/ScalaLoggingSpec.scala)

### Using Markers and Marker Contexts

The SLF4J API has a concept of markers, which act to enrich logging messages and mark out messages as being of special interest.  Markers are especially useful for triggering and filtering -- for example, [OnMarkerEvaluator](https://logback.qos.ch/manual/appenders.html#OnMarkerEvaluator) can send an email when a marker is seen, or particular flows can be marked out to their own appenders.

The Logger API provides access to markers through the `play.api.MarkerContext` trait.

You can create a MarkerContext with the Logger by using the `MarkerContext.apply` method:

@[logging-marker-context](code/ScalaLoggingSpec.scala)

You can also provide a typed MarkerContext by extending from `DefaultMarkerContext`:

@[logging-default-marker-context](code/ScalaLoggingSpec.scala)

Once a MarkerContext has been created, it can be used with a logging statement, either explicitly:

@[logging-log-info-with-explicit-markercontext](code/ScalaLoggingSpec.scala)

Or implicitly:

@[logging-log-info-with-implicit-markercontext](code/ScalaLoggingSpec.scala)

For convenience, there is an implicit conversion available from a `Marker` to a `MarkerContext`:

@[logging-log-info-with-implicit-conversion](code/ScalaLoggingSpec.scala)

Markers can be extremely useful, because they can carry contextual information across threads where MDC may not be available, by using a MarkerContext as an implicit parameter to methods to provide a logging context.  For example, using [Logstash Logback Encoder](https://github.com/logstash/logstash-logback-encoder#loggingevent_custom_event) and an [implicit conversion chain](https://docs.scala-lang.org/tutorials/FAQ/chaining-implicits.html), request information can be encoded into logging statements automatically:

@[logging-request-context-trait](code/ScalaLoggingSpec.scala)

And then used in a controller and carried through `Future` that may use different execution contexts:

@[logging-log-info-with-request-context](code/ScalaLoggingSpec.scala)

Note that marker contexts are also very useful for "tracer bullet" style logging, where you want to log on a specific request without explicitly changing log levels.  For example, you can add a marker only when certain conditions are met:

@[logging-log-trace-with-tracer-controller](code/ScalaLoggingSpec.scala)

And then trigger logging with the following TurboFilter in `logback.xml`: 

```xml
<turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
  <Name>TRACER_FILTER</Name>
  <Marker>TRACER</Marker>
  <OnMatch>ACCEPT</OnMatch>
</turboFilter>
```

At which point you can dynamically set debug statements in response to input.

For more information about using Markers in logging, see [TurboFilters](https://logback.qos.ch/manual/filters.html#TurboFilter) and [marker based triggering](https://logback.qos.ch/manual/appenders.html#OnMarkerEvaluator) sections in the Logback manual.

### Logging patterns

Effective use of loggers can help you achieve many goals with the same tool:

@[logging-pattern-mix](code/ScalaLoggingSpec.scala)

This example uses [[action composition|ScalaActionsComposition]] to define an `AccessLoggingAction` that will log request data to a logger named "access." The `Application` controller uses this action and it also uses its own logger (named after its class) for application events. In configuration you could then route these loggers to different appenders, such as an access log and an application log.

The above design works well if you want to log request data for only specific actions. To log all requests, it's better to use a [[filter|ScalaHttpFilters]]:

@[logging-pattern-filter](code/ScalaLoggingSpec.scala)

In the filter version we've added the response status to the log request by logging when the `Future[Result]` completes.

## Configuration
See [[configuring logging|SettingsLogger]] for details on configuration. 
