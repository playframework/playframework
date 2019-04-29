<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# The Typesafe Config API

Play uses the [Typesafe config library](https://github.com/typesafehub/config) as the configuration library. If you're not familiar with Typesafe config, you may also want to read the documentation on [[configuration file syntax and features|ConfigFile]].

## Accessing the configuration

Typically, you'll obtain a `Config` object through [[Dependency Injection|JavaDependencyInjection]], or simply by passing an instance of `Config` to your component:

@[](code/javaguide/config/MyController.java)

## API documentation

Since Play just uses `Config` object, you can [see the javadoc for the class](https://static.javadoc.io/com.typesafe/config/1.3.1/com/typesafe/config/Config.html) to see what you can do and how to access configuration data.