<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Using LESS CSS

[LESS CSS](http://lesscss.org/) is a dynamic stylesheet language. It allows considerable flexibility in the way you write CSS files including support for variables, mixins and more.

Compilable assets in Play must be defined in the `app/assets` directory. They are handled by the build process, and LESS sources are compiled into standard CSS files. The generated CSS files are distributed as standard resources into the same `public/` folder as the unmanaged assets, meaning that there is no difference in the way you use them once compiled.

For example, a LESS source file at `app/assets/stylesheets/main.less` will be available as a standard resource at `public/stylesheets/main.css`.  Play will compile `main.less` automatically.  Other LESS files need to be included in your `build.sbt` file:

```scala
includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
```

LESS sources are compiled automatically during an `assets` command, or when you refresh any page in your browser while you are running in development mode. Any compilation errors will be displayed in your browser:

[[images/lessError.png]]

## Working with partial LESS source files

You can split your LESS source into several libraries and use the LESS `import` feature. 

To prevent library files from being compiled individually (or imported) we need them to be skipped by the compiler. To do this partial source files can be prefixed with the underscore (`_`) character, for example: `_myLibrary.less`. The following configuration enables the compiler to ignore partials:

```scala
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
```


## Layout

Here is an example layout for using LESS in your project:

```
app
 └ assets
    └ stylesheets
       └ main.less
       └ utils
          └ reset.less
          └ layout.less    
```

With the following `main.less` source:

```css
@import "utils/reset.less";
@import "utils/layout.less";

h1 {
    color: red;
}
```

The resulting CSS file will be compiled as `public/stylesheets/main.css` and you can use this in your template as any regular public asset.

```html
<link rel="stylesheet" href="@routes.Assets.at("stylesheets/main.css")">
```

## Using LESS with Bootstrap

[Bootstrap](http://getbootstrap.com/css/) is a very popular library used in conjunction with LESS.

To use Bootstrap you can use its [WebJar](http://www.webjars.org/) by adding it to your library dependencies. For example, within a `build.sbt` file:

```scala
libraryDependencies += "org.webjars" % "bootstrap" % "3.2.0"
```

sbt-web will automatically extract WebJars into a lib folder relative to your asset's target folder. Therefore to use Bootstrap you can import relatively e.g.:

```less
@import "lib/bootstrap/less/bootstrap.less";

h1 {
  color: @font-size-h1;
}
```

## Enablement and Configuration

LESS compilation is enabled by simply adding the plugin to your plugins.sbt file when using the `PlayJava` or `PlayScala` plugins:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")
```

The plugin's default configuration is normally sufficient. However please refer to the [plugin's documentation](https://github.com/sbt/sbt-less#sbt-less) for information on how it may be configured.

