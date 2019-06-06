<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring logging

Play uses SLF4J for logging, backed by [Logback](https://logback.qos.ch/) as its default logging engine.  See the [Logback documentation](https://logback.qos.ch/manual/configuration.html) for details on configuration.

## Default configuration

In dev mode Play uses the following default configuration:

@[](/confs/play-logback/logback-play-dev.xml)

Play uses the following default configuration in production:

@[](/confs/play-logback/logback-play-default.xml)

A few things to note about these configurations:

* These default configs specify only a console logger which outputs only 10 lines of an exception stack trace.
* Play uses ANSI color codes by default in level messages.
* For production, the default config puts the console logger behind the logback [AsyncAppender](https://logback.qos.ch/manual/appenders.html#AsyncAppender).  For details on the performance implications on this, see this [blog post](https://blog.takipi.com/how-to-instantly-improve-your-java-logging-with-7-logback-tweaks/).
* In order to guarantee that logged messages have had a chance to be processed by asynchronous appenders (including the TCP appender) and ensure background threads have been stopped, you'll need to cleanly shut down logback when your application exits. For details on a shutdown hook, see this [documentation](https://logback.qos.ch/manual/configuration.html#shutdownHook). Also [you must specify](https://jira.qos.ch/browse/LOGBACK-1090) DelayingShutdownHook explicitly: `<shutdownHook class="ch.qos.logback.core.hook.DelayingShutdownHook"/>` .

To add a file logger, add the following appender to your `conf/logback.xml` file:

```xml
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>${application.home:-.}/logs/application.log</file>
    <encoder>
        <pattern>%date [%level] from %logger in %thread - %message%n%xException</pattern>
    </encoder>
</appender>
```

Optionally use the async appender to wrap the `FileAppender`:
```xml
<appender name="ASYNCFILE" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE" />
</appender>
```

Add the necessary appender(s) to the root:
```xml
<root level="WARN">
    <appender-ref ref="ASYNCFILE" />
    <appender-ref ref="ASYNCSTDOUT" />
</root>
```

## Security Logging

A security marker has been added for security related operations in Play, and failed security checks now log  at WARN level, with the security marker set.  This ensures that developers always know why a particular request is failing, which is important now that security filters are enabled by default in Play.

The security marker also allows security failures to be triggered or filtered distinct from normal logging.  For example, to disable all logging with the SECURITY marker set, add the following lines to the `logback.xml` file:

```xml
<turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
    <Marker>SECURITY</Marker>
    <OnMatch>DENY</OnMatch>
</turboFilter>
```

In addition, log events using the security marker can also trigger a message to a Security Information & Event Management (SEIM) engine for further processing.

## Using a custom application loader

Note that when using a custom application loader that does not extend the default `GuiceApplicationLoader` (for example when using [[compile-time dependency injection|ScalaCompileTimeDependencyInjection]]), the `LoggerConfigurator` needs to be manually invoked to pick up your custom configuration. You can do this with code like the following:

@[basicextended](../../scalaGuide/main/dependencyinjection/code/CompileTimeDependencyInjection.scala)

## Custom configuration

For any custom configuration, you will need to specify your own Logback configuration file.

### Using a configuration file from project source

You can provide a default logging configuration by providing a file `conf/logback.xml`.

### Using an external configuration file

You can also specify a configuration file via a System property.  This is particularly useful for production environments where the configuration file may be managed outside of your application source.

> **Note**: The logging system gives top preference to configuration files specified by system properties, secondly to files in the `conf` directory, and lastly to the default. This allows you to customize your application's logging configuration and still override it for specific environments or developer setups.

#### Using `-Dlogger.resource`

Specify a configuration file to be loaded from the classpath:

```
$ start -Dlogger.resource=prod-logger.xml
```

#### Using `-Dlogger.file`

Specify a configuration file to be loaded from the file system:

```
$ start -Dlogger.file=/opt/prod/logger.xml
```

> **Note**: To see which file is being used, you can set a system property to debug it: `-Dlogback.debug=true`.

### Examples

Here's an example of configuration that uses a rolling file appender, as well as a separate appender for outputting an access log:

```xml
<configuration>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${application.home:-.}/logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- Daily rollover with compression -->
            <fileNamePattern>${application.home:-.}/logs/application-log-%d{yyyy-MM-dd}.gz</fileNamePattern>
            <!-- keep 30 days worth of history -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date{yyyy-MM-dd HH:mm:ss ZZZZ} [%level] from %logger in %thread - %message%n%xException</pattern>
        </encoder>
    </appender>

    <appender name="SECURITY_FILE" class="ch.qos.logback.core.FileAppender">
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
                <marker>SECURITY</marker>
            </evaluator>
            <OnMismatch>DENY</OnMismatch>
            <OnMatch>ACCEPT</OnMatch>
        </filter>
        <file>${application.home:-.}/logs/security.log</file>
        <encoder>
            <pattern>%date [%level] [%marker] from %logger in %thread - %message%n%xException</pattern>
        </encoder>
    </appender>

    <appender name="ACCESS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${application.home:-.}/logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover with compression -->
            <fileNamePattern>${application.home:-.}/logs/access-log-%d{yyyy-MM-dd}.gz</fileNamePattern>
            <!-- keep 1 week worth of history -->
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date{yyyy-MM-dd HH:mm:ss ZZZZ} %message%n</pattern>
            <!-- this quadruples logging throughput -->
            <immediateFlush>false</immediateFlush>
        </encoder>
    </appender>

    <!-- additivity=false ensures access log data only goes to the access log -->
    <logger name="access" level="INFO" additivity="false">
        <appender-ref ref="ACCESS_FILE" />
    </logger>

    <root level="INFO">
        <appender-ref ref="FILE"/>
        <appender-ref ref="SECURITY_FILE"/>
    </root>

</configuration>
```

This demonstrates a few useful features:

- It uses `RollingFileAppender` which can help manage growing log files. See more [details here](https://logback.qos.ch/manual/appenders.html#SizeAndTimeBasedRollingPolicy).
- It writes log files to a directory external to the application so they will not affected by upgrades, etc.
- The `FILE` appender uses an expanded message format that can be parsed by third party log analytics providers such as Sumo Logic.
- The `access` logger is routed to a separate log file using the `ACCESS_FILE` appender.
- Any log messages sent with the "SECURITY" marker attached are logged to the `security.log` file using the [EvaluatorFilter](https://logback.qos.ch/manual/filters.html#evalutatorFilter) and the [OnMarkerEvaluator](https://logback.qos.ch/manual/appenders.html#OnMarkerEvaluator).
- All loggers are set to a threshold of `INFO` which is a common choice for production logging.

> **Note**: the `file` tag is optional and you can omit it if you want to avoid file renaming. See [Logback docs](https://logback.qos.ch/codes.html#renamingError) for more information.

## Including Properties

By default, only the property `application.home` is exported to the logging framework, meaning that files can be referenced relative to the Play application:

```
 <file>${application.home:-}/example.log</file>
```

If you want to reference properties that are defined in the `application.conf` file, you can add `play.logger.includeConfigProperties=true` to your application.conf file.  When the application starts, all properties defined in configuration will be available to the logger:

```
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>context = ${my.property.defined.in.application.conf} %message%n</pattern>
    </encoder>
</appender>
```

## Akka logging configuration

Akka system logging can be done by changing the `akka` logger to INFO.

```xml
<!-- Set logging for all Akka library classes to INFO -->
<logger name="akka" level="INFO" />
<!-- Set a specific actor to DEBUG -->
<logger name="actors.MyActor" level="DEBUG" />
```

You may also wish to configure an appender for the Akka loggers that includes useful properties such as thread and actor address.  For more information about configuring Akka's logging, including details on Logback and Slf4j integration, see the [Akka documentation](https://doc.akka.io/docs/akka/2.5/logging.html?language=scala).

## Using a Custom Logging Framework

Play uses Logback by default, but it is possible to configure Play to use another logging framework as long as there is an SLF4J adapter for it.  To do this, the `PlayLogback` sbt plugin must be disabled using `disablePlugins`:

```scala
lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLogback)
```

From there, a custom logging framework can be used.  Here, Log4J 2 is used as an example.

```scala
libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.4.1",
  "org.apache.logging.log4j" % "log4j-api" % "2.4.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.4.1"
)
```

Once the libraries and the SLF4J adapter are loaded, the `log4j.configurationFile` system property can be set on the command line as usual.

If custom configuration depending on Play's mode is required, you can do additional customization with the `LoggerConfigurator`.  To do this, add a `logger-configurator.properties` to the classpath, with

```properties
play.logger.configurator=Log4J2LoggerConfigurator
```

And then extend LoggerConfigurator with any customizations:

Java
: @[log4j2-class](code/JavaLog4JLoggerConfigurator.java)

Scala
: @[log4j2-class](code/Log4j2LoggerConfigurator.scala)
