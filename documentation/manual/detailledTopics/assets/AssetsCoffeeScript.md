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

CoffeeScript compilation can be configured in your project’s `Build.scala` file (in the settings part of the `PlayProject`). The only option currently supported is *bare* mode.

```
coffeescriptOptions := Seq("bare")
```
> Note there is a new experimental option which lets you use the native coffee script compiler. The benefit is that it's way faster, the disadvantage is that it's an external dependency. If you want to try this, add this to your settings:

```
coffeescriptOptions := Seq("native", "/usr/local/bin/coffee -p")
```


By default, the JavaScript code is generated inside a top-level function safety wrapper, preventing it from polluting the global scope. The `bare` option removes this function wrapper.

> **Next:** [[Using LESS CSS | AssetsLess]]
