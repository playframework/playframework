<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Using Sass

[Sass](http://sass-lang.com/) is a dynamic stylesheet language. It allows considerable flexibility in the way you write CSS files including support for variables, mixins and more.

Compilable assets in Play are typically defined in the `app/assets` directory. They are handled by the build process, and Sass sources are compiled into standard CSS files. The generated CSS files are distributed as standard resources into the same `public/` folder as the unmanaged assets, meaning that there is no difference in the way you use them once compiled.

For example, Sass source file `app/assets/stylesheets/main.scss` will be available as standard CSS resource, at `public/stylesheets/main.css`.

Sass sources are compiled automatically during an `assets` command, or when you refresh any page in your browser while you are running in development mode. Any compilation errors will be displayed in your browser:

[[images/sassError.png]]

## Working with partial Sass source files

Any Sass file (`*.scss`/`*.sass`) will automatically be compiled. The Sass plugin will automatically determine which Sass syntax is being used (indented or not) based on the filename. A file who's name starts with an `_` will not be compiled separately. However, such files can be included in other Sass files by using the standard Sass import feature.

## Layout

Below an example layout for using Sass in your project is given:

```
app
 └ assets
   └ stylesheets
     └ main.scss
     └ utils
       └ _reset.scss
       └ _layout.scss
```

Given the following `main.scss` source:

```scss
@import "utils/reset";
@import "utils/layout";

h1 {
  color: red;
}
```

The Sass file outlined above, will be compiled into `public/stylesheets/main.css`. Hence, this file can be used in your template as any regular public asset:

```html
<link rel="stylesheet" href="@routes.Assets.at("stylesheets/main.css")">
```

## Mixing Sass and web-jars

[WebJars](https://www.webjars.org) enable us to depend on client libraries without pulling all dependencies into our own code base manually.

Compass is a library containing all sorts of reusable functions and mixins for Sass. Unfortunately, this library is targeted towards the Ruby implementation of Sass. There is a number of useful mixins that can be extracted from it. Fortunately, these mixins are wrapped in a web-jar.

To include these compass mixins in your project is as easy as including the web-jar dependency in your library dependencies. For example, within a `build.sbt` file:

```scala
libraryDependencies += "org.webjars.bower" % "compass-mixins" % "0.12.7"
```

sbt-web will automatically extract WebJars into a `lib` directory relative to your asset's target directory. Therefore to use the Compass mixins you can import the mixins by:

```scss
@import "lib/compass-mixins/lib/compass";

table.ellipsed-table {
  tr td {
    max-width: 100px;
    @include ellipsis();
  }
}
```

The same idea can be used to include other Sass libraries, for instance the [official Sass port of bootstrap](https://github.com/twbs/bootstrap-sass). To include the WebJar use:

```scala
libraryDependencies += "org.webjars.bower" % "bootstrap-sass" % "3.3.6"
```

Then to use it in your project, you can use:

```scss
@import "lib/bootstrap-sass/assets/stylesheets/bootstrap";
```

## Enablement and Configuration

Sass compilation is enabled by simply adding the sbt-sassify plugin to your plugins.sbt file when using the `PlayJava` or `PlayScala` plugins:

```scala
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.11")
```

The plugin's default configuration should normally be sufficient. However please refer to the [plugin's documentation](https://github.com/irundaia/sbt-sassify#options) for information on how it may be configured as well as its latest version.

