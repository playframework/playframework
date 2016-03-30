<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring logging

Play uses SLF4J for logging, backed by [Logback](http://logback.qos.ch/) as its default logging engine.  See the [Logback documentation](http://logback.qos.ch/manual/configuration.html) for details on configuration.

## Default configuration

Play uses the following default configuration in production:

@[](/confs/play-logback/logback-play-default.xml)

A few things to note about this configuration:

* This specifies a file appender that writes to `logs/application.log`.
* The file logger logs full exception stack traces, while the console logger only logs 10 lines of an exception stack trace.
* Play uses ANSI color codes by default in level messages.
* Play puts both the console and the file logger behind the logback [AsyncAppender](http://logback.qos.ch/manual/appenders.html#AsyncAppender).  For details on the performance implications on this, see this [blog post](https://blog.takipi.com/how-to-instantly-improve-your-java-logging-with-7-logback-tweaks/).

## Custom configuration

For any custom configuration, you will need to specify your own Logback configuration file.

### Using a configuration file from project source

You can provide a default logging configuration by providing a file `conf/logback.xml`.

### Using an external configuration file

You can also specify a configuration file via a System property.  This is particularly useful for production environments where the configuration file may be managed outside of your application source.

> Note: The logging system gives top preference to configuration files specified by system properties, secondly to files in the `conf` directory, and lastly to the default. This allows you to customize your application's logging configuration and still override it for specific environments or developer setups.

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

### Examples

Here's an example of configuration that uses a rolling file appender, as well as a seperate appender for outputting an access log:

```xml
<configuration>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${user.dir}/web/logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- Daily rollover with compression -->
            <fileNamePattern>application-log-%d{yyyy-MM-dd}.gz</fileNamePattern>
            <!-- keep 30 days worth of history -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date{yyyy-MM-dd HH:mm:ss ZZZZ} [%level] from %logger in %thread - %message%n%xException</pattern>
        </encoder>
    </appender>

    <appender name="ACCESS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${user.dir}/web/logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover with compression -->
            <fileNamePattern>access-log-%d{yyyy-MM-dd}.gz</fileNamePattern>
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
    </root>

</configuration>

```

This demonstrates a few useful features:

- It uses `RollingFileAppender` which can help manage growing log files.
- It writes log files to a directory external to the application so they aren't affected by upgrades, etc.
- The `FILE` appender uses an expanded message format that can be parsed by third party log analytics providers such as Sumo Logic.
- The `access` logger is routed to a separate log file using the `ACCESS_FILE_APPENDER`.
- All loggers are set to a threshold of `INFO` which is a common choice for production logging.  

## Akka logging configuration

Akka system logging can be done by changing the `akka` logger to INFO.

```xml
<!-- Set logging for all Akka library classes to INFO -->
<logger name="akka" level="INFO" />
<!-- Set a specific actor to DEBUG -->
<logger name="actors.MyActor" level="DEBUG" />
```

You may also wish to configure an appender for the Akka loggers that includes useful properties such as thread and actor address.  For more information about configuring Akka's logging, including details on Logback and Slf4j integration, see the [Akka documentation](http://doc.akka.io/docs/akka/current/scala/logging.html).

## Using a Custom Logging Framework

Play uses Logback by default, but it is possible to configure Play to use another logging framework as long as there is an SLF4J adapter for it.  To do this, the `PlayLogback` SBT plugin must be disabled using `disablePlugins`:

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

```scala
import java.io.File
import java.net.URL

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core._
import org.apache.logging.log4j.core.config.Configurator

import play.api.{Mode, Environment, LoggerConfigurator}

class Log4J2LoggerConfigurator extends LoggerConfigurator {

  override def init(rootPath: File, mode: Mode.Mode): Unit = {
    val properties = Map("application.home" -> rootPath.getAbsolutePath)
    val resourceName = if (mode == Mode.Dev) "log4j2-dev.xml" else "log4j2.xml"
    val resourceUrl = Option(this.getClass.getClassLoader.getResource(resourceName))
    configure(properties, resourceUrl)
  }

  override def shutdown(): Unit = {
    val context = LogManager.getContext().asInstanceOf[LoggerContext]
    Configurator.shutdown(context)
  }

  override def configure(env: Environment): Unit = {
    val properties = Map("application.home" -> env.rootPath.getAbsolutePath)
    val resourceUrl = env.resource("log4j2.xml")
    configure(properties, resourceUrl)
  }

  override def configure(properties: Map[String, String], config: Option[URL]): Unit = {
    val context =  LogManager.getContext(false).asInstanceOf[LoggerContext]
    context.setConfigLocation(config.get.toURI)
  }
}
```
