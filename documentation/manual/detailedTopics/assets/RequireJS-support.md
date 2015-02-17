<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# RequireJS

According to [RequireJS](http://requirejs.org/)' website 

> RequireJS is a JavaScript file and module loader. It is optimized for in-browser use, but it can be used in other JavaScript environments... Using a modular script loader like RequireJS will improve the speed and quality of your code.

What this means in practice is that one can use [RequireJS](http://requirejs.org/) to modularize your JavaScript. RequireJS achieves this by implementing a semi-standard API called [Asynchronous Module Definition](http://wiki.commonjs.org/wiki/Modules/AsynchronousDefinition) (other similar ideas include [CommonJS](http://www.commonjs.org/) ). Using AMD makes it is possible to resolve and load javascript modules on the _client side_ while allowing server side _optimization_. For server side optimization module dependencies may be minified and combined using [UglifyJS 2](https://github.com/mishoo/UglifyJS2#uglifyjs-2).

By convention RequireJS expects a main.js file to bootstrap its module loader.

## Deployment

The RequireJS optimizer shouldn't generally kick-in until it is time to perform a deployment i.e. by running the `start`, `stage` or `dist` tasks.

If you're using WebJars with your build then the RequireJS optimizer plugin will also ensure that any JavaScript resources referenced from within a WebJar are automatically referenced from the [jsdelivr](http://www.jsdelivr.com) CDN. In addition if any `.min.js` file is found then that will be used in place of `.js`. An added bonus here is that there is no change required to your html!

## Enablement and Configuration

RequireJS optimization is enabled by simply adding the plugin to your plugins.sbt file when using the `PlayJava` or `PlayScala` plugins:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")
```

To add the plugin to the asset pipeline you can declare it as follows (assuming just the one plugin for the pipeline - add others into the sequence such as digest and gzip as required):

```scala
pipelineStages := Seq(rjs)
```

A standard build profile for the RequireJS optimizer is provided and should suffice for most projects. However please refer to the [plugin's documentation](https://github.com/sbt/sbt-rjs#sbt-rjs) for information on how it may be configured.

Note that RequireJS performs a lot of work and while it works when executed in-JVM under Trireme, you will be best to use Node.js as the js-engine from a performance perspective. For convenience you can set the `sbt.jse.engineType` property in `SBT_OPTS`. For example on Unix:

```bash
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
```

Please refer to the [plugin's documentation](https://github.com/sbt/sbt-rjs#sbt-rjs) for information on how it may be configured.
