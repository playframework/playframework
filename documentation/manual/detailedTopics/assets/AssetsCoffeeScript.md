<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Using CoffeeScript

[CoffeeScript](http://coffeescript.org/) is a small and elegant language that compiles into JavaScript. It provides a nice syntax for writing JavaScript code.

Compiled assets in Play must be defined in the `app/assets` directory. They are handled by the build process and CoffeeScript sources are compiled into standard JavaScript files. The generated JavaScript files are distributed as standard resources into the same `public/` folder as other unmanaged assets, meaning that there is no difference in the way you use them once compiled.

For example a CoffeeScript source file `app/assets/javascripts/main.coffee` will be available as a standard JavaScript resource, at `public/javascripts/main.js`.

CoffeeScript sources are compiled automatically during an `assets` command, or when you refresh any page in your browser while you are running in development mode. Any compilation errors will be displayed in your browser:

[[images/coffeeError.png]]

## Layout

Here is an example layout for using CoffeeScript in your projects:

```
app
 └ assets
    └ javascripts
       └ main.coffee   
```

You can use the following syntax to use the compiled JavaScript file in your template:

```html
<script src="@routes.Assets.at("javascripts/main.js")">
```

## Enablement and Configuration

CoffeeScript compilation is enabled by simply adding the plugin to your plugins.sbt file when using the `PlayJava` or `PlayScala` plugins:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
```

The plugin's default configuration is normally sufficient. However please refer to the [plugin's documentation](https://github.com/sbt/sbt-coffeescript#sbt-coffeescript) for information on how it may be configured.
