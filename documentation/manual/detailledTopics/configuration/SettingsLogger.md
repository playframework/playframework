# Configuring logging

Play 2.0 uses [[logback | http://logback.qos.ch/]] as its logging engine.

## Configuration logging level in application.conf

The easiest way to configure the logging level is to use the `logger` key in your `conf/application.conf` file.

Play defines a default `application` logger for your application, which is automatically used when you use the default `Logger` operations.

```properties
# Root logger:
logger=ERROR

# Logger used by the framework:
logger.play=INFO

# Logger provided to your application:
logger.application=DEBUG
```

The root logger configuration affects all log calls, rather than requiring custom logging levels. Additionally, if you want to enable the logging level for a specific library, you can specify it here. For example to enable `TRACE` log level for Spring, you could add:

```properties
logger.org.springframework=TRACE
```

## Configuring logback

The default is to define two appenders, one dispatched to the standard out stream, and the other to the `logs/application.log` file.

If you want to fully customize logback, just define a `conf/application-logger.xml` configuration file. Here is the default configuration file used by Play:

```xml
<configuration>
    
  <conversionRule conversionWord="coloredLevel" converterClass="play.api.Logger$ColoredLevel" />
  
  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
     <file>${application.home}/logs/application.log</file>
     <encoder>
       <pattern>%date - [%level] - from %logger in %thread %n%message%n%xException%n</pattern>
     </encoder>
   </appender>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%coloredLevel %logger{15} - %message%n%xException{5}</pattern>
    </encoder>
  </appender>
  
  <logger name="play" level="INFO" />
  <logger name="application" level="INFO" />

  <root level="ERROR">
    <appender-ref ref="STDOUT" />
    <appender-ref ref="FILE" />
  </root>
  
</configuration>
```

## Changing the logback configuration file

You can also specify another logback configuration file via a System property. It is particulary useful when running in production.

### Using `-Dlogger.resource`

Specify another logback configuration file to be loaded from the classpath:

```
$ start -Dlogger.resource=prod-logger.xml
```

### Using `-Dlogger.file`

Specify another logback configuration file to be loaded from the file system:

```
$ start -Dlogger.file=/opt/prod/logger.xml
```

### Using `-Dlogger.url`

Specify another logback configuration file to be loaded from an URL:

```
$ start -Dlogger.url=http://conf.mycompany.com/logger.xml
```
