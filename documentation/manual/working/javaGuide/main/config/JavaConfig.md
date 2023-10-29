<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# The Typesafe Config API

Play uses the [Typesafe config library](https://github.com/lightbend/config) as the configuration library. If you're not familiar with Typesafe config, you may also want to read the documentation on [[configuration file syntax and features|ConfigFile]].

## Accessing the configuration

Typically, you'll obtain a `Config` object through [[Dependency Injection|JavaDependencyInjection]], or simply by passing an instance of `Config` to your component:

@[](code/javaguide/config/MyController.java)

## API documentation

Since Play just uses `Config` object, you can [see the javadoc for the class](https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html) to see what you can do and how to access configuration data.