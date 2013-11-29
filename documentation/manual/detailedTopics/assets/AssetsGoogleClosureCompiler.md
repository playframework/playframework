# Using Google Closure Compiler

The [Closure Compiler](http://code.google.com/p/closure-compiler/) is a tool for making JavaScript download and run faster. It is a true compiler for JavaScript - though instead of compiling from a source language to machine code, it compiles JavaScript to better JavaScript. It parses your JavaScript, analyzes it, removes dead code and rewrites and minimizes what’s left.

Any JavaScript file present in `app/assets` will be parsed by Google Closure compiler, checked for errors and dependencies and minified if activated in the build configuration.

## Check JavaScript sanity

JavaScript code is compiled during the `compile` command, as well as automatically when modified. Error are shown in the browser just like any other compilation error.

[[images/ClosureError.png]]

## Minification

A minified file is also generated, where `.js` is replaced by `.min.js`. In our example, it would be `test.min.js`. If you want to use the minified file instead of the regular file, you need to change the script source attribute in your HTML.

## Entry Points

By default, any JavaScript file not prepended by an underscore will be compiled. This behavior can be changed in `build.sbt` by overriding the `javascriptEntryPoints` key. This key holds a `PathFinder`.

For example, to compile only `.js` file from the `app/assets/javascripts/main` directory:

```
javascriptEntryPoints <<= baseDirectory(base =>
    base / "app" / "assets" / "javascripts" / "main" ** "*.js"
)
```

The default definition is:

```
javascriptEntryPoints <<= (sourceDirectory in Compile)(base =>
   ((base / "assets" ** "*.js") --- (base / "assets" ** "_*")).get
)
```

## Options

ClosureCompiler compilation can be configured in your project’s `build.sbt` file. There are several currently supported options:

- *advancedOptimizations* Achieves extra compressions by being more aggressive in the ways that it transforms code and renames symbols. However, this more aggressive approach means that you must take greater care when you use ADVANCED_OPTIMIZATIONS to ensure that the output code works the same way as the input code.
- *checkCaja* Checks Caja control structures.
- *checkControlStructures* Checks for invalid control structures.
- *checkTypes* Checks for invalid types.
- *checkSymbols* Checks for invalid symbols.
- *ecmascript5* Sets the input- and output-language to the newer ECMAScript version 5. Might break code on older browsers.

Example:

```
closureCompilerOptions += "ecmascript5"
```

> **Next:** [[Using require.js to manage dependencies | RequireJS-support]]
