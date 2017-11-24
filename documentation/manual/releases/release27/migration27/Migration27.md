<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.7 Migration Guide

This is a guide for migrating from Play 2.6 to Play 2.7. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.6 Migration Guide|Migration26]].

### `play.Logger` deprecated

`play.Logger` has been deprecated in favor of using SLF4J directly. You can create an SLF4J logger with `private static final Logger logger = LoggerFactory.getLogger(YourClass.class);`. If you'd like a more concise solution, you may also consider [Project Lombok's `@Slf4j` annotation](https://projectlombok.org/features/log).

If you have a in your logback.xml referencing the `application` logger, you may remove it.

    <logger name="application" level="DEBUG" />

Each logger should have a unique name matching the name of the class it is in. In this way, you can configure a different log level for each class. You can also set the log level for a given package. E.g. to set the log level for all of Play's internal classes to the info level, you can set:

    <logger name="play" level="INFO" />

### Evolutions comment syntax changes

Play Evolutions now properly supports SQL92 comment syntax. This means you can write evolutions using `--` at the beginning of a line instead of `#` wherever you choose. Newly generated evolutions using the Evolutions API will now also use SQL92-style comment syntax in all areas. Documentation has also been updated accordingly to prefer the SQL92 style, though the older comment style is still fully supported.

### `application/javascript` as default content type
`application/javascript` is now the default content-type returned for Javascript instead of `text/javascript`. 