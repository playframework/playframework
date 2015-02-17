<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Using JSHint

From its [website documentation](http://www.jshint.com/about/):

> JSHint is a community-driven tool to detect errors and potential problems in JavaScript code and to enforce your team's coding conventions. It is very flexible so you can easily adjust it to your particular coding guidelines and the environment you expect your code to execute in.

Any JavaScript file present in `app/assets` will be processed by JSHint and checked for errors.

## Check JavaScript sanity

JavaScript code is compiled during the `assets` command as well as when the browser is refreshed during development mode. Errors are shown in the browser just like any other compilation error.

## Enablement and Configuration

JSHint processing is enabled by simply adding the plugin to your plugins.sbt file when using the `PlayJava` or `PlayScala` plugins:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.0")
```

The plugin's default configuration is normally sufficient. However please refer to the [plugin's documentation](https://github.com/sbt/sbt-jshint#sbt-jshint) for information on how it may be configured.

