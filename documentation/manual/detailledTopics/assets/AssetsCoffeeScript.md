# Using CoffeeScript

[CoffeeScript](http://jashkenas.github.com/coffee-script/) is a small and elegant language that compiles into JavaScript. It provides a nicer syntax for writing JavaScript code.

Compiled assets in Play must be defined in the `app/assets` directory. They are handled by the build process, and CoffeeScript sources are compiled into standard JavaScript files. The generated JavaScript files are distributed as standard resources into the same `public/` folder as other unmanaged assets, meaning that there is no difference in the way you use them once compiled.

> Note that managed resources are not copied directly into your application’s `public` folder, but maintained in a separate folder in `target/scala-2.x.x/resources_managed`.

For example a CoffeeScript source file `app/assets/javascripts/main.coffee` will be available as a standard JavaScript resource, at `public/javascripts/main.js`.

CoffeeScript sources are compiled automatically during a `compile` command, or when you refresh any page in your browser while you are running in development mode. Any compilation errors will be displayed in your browser:

[[images/coffeeError.png]]

## Layout

Here is an example layout for using CoffeeScript in your projects:

```
app
 └ assets
    └ javascripts
       └ main.coffee   
```

Two JavaScript files will be compiled: `public/javascripts/main.js` and `public/javascripts/main.min.js`. The first one is a readable file useful in development, and the second one a minified file that you can use in production. You can use either one in your template:

```html
<script src="@routes.Assets.at("javascripts/main.js")">
```

```html
<script src="@routes.Assets.at("javascripts/main.min.js")">
```

## Options

CoffeeScript compilation can be configured in your project’s `Build.scala` file (in the settings part of the `PlayProject`). There are 3 options currently supported.

### "bare"

Compile the JavaScript without the top-level function safety wrapper.
By default, the JavaScript code is generated inside a top-level function safety wrapper, preventing it from polluting the global scope. The `bare` option removes this function wrapper.

```
coffeescriptOptions := Seq("bare")
```

### "map"

Generate source maps alongside the compiled JavaScript files. Adds sourceMappingURL directives to the JavaScript as well.

This option will output 3 files:

* generated JavaScript file (source.js),
* a copy of the original source file (source.coffee),
* a map file to link source and generated JavaScript (source.map).

```
coffeescriptOptions := Seq("map")
```

### "native"

There is a new experimental option which lets you use the native coffee script compiler. The benefit is that it's way faster, the disadvantage is that it's an external dependency.

> Note the "native" option must be followed by the path to the CoffeeScript compiler, and results should be printed to stdout (with option '-p').
> Moreover, options (such as "bare" or "map") are ignored while using a native compiler.

```
coffeescriptOptions := Seq("native", "/usr/local/bin/coffee -p")
```

> **Next:** [[Using LESS CSS | AssetsLess]]
