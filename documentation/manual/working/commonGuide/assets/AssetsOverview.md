<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Working with public assets

Serving a public resource in Play is the same as serving any other HTTP request. It uses the same routing as regular resources using the controller/action path to distribute CSS, JavaScript or image files to the client.

## The public/ folder

By convention public assets are stored in the `public` folder of your application. This folder can be organized the way that you prefer. We recommend the following organization:

```
public
 └ javascripts
 └ stylesheets
 └ images
```

If you follow this structure it will be simpler to get started, but nothing stops you to modifying it once you understand how it works.

## WebJars

[WebJars](https://www.webjars.org/) provide a convenient and conventional packaging mechanism that is a part of sbt. For example you can declare that you will be using the popular [Bootstrap library](http://getbootstrap.com/) simply by adding the following dependency in your build file:

```scala
libraryDependencies += "org.webjars" % "bootstrap" % "3.3.6"
```

WebJars are automatically extracted into a `lib` folder relative to your public assets for convenience. For example, if you declared a dependency on [RequireJs](https://requirejs.org/) then you can reference it from a view using a line like:

```html
<script data-main="@routes.Assets.at("javascripts/main.js")" type="text/javascript" src="@routes.Assets.at("lib/requirejs/require.js")"></script>
```

Note the `lib/requirejs/require.js` path. The `lib` folder denotes the extracted WebJar assets, the `requirejs` folder corresponds to the WebJar artifactId, and the `require.js` refers to the required asset at the root of the WebJar. To clarify, the `requirejs` webjar dependency is declared at your build file like:

```scala
libraryDependencies += "org.webjars" % "requirejs" % "2.2.0"
```

## How are public assets packaged?

During the build process, the contents of the `public` folder are processed and added to the application classpath.

When you package your application, all assets for the application, including all sub projects, are aggregated into a single jar, in `target/my-first-app-1.0.0-assets.jar`.  This jar is included in the distribution so that your Play application can serve them.  This jar can also be used to deploy the assets to a CDN or reverse proxy.

## The Assets controller

Play comes with a built-in controller to serve public assets. By default, this controller provides caching, ETag, gzip and compression support. There are two different styles that the Assets controller supports: the first is to use Play's configuration, and the second is to use pass the assets path directly to the controller.

### Binding the Assets components

If you are using runtime dependency injection, Play already provides bindings in the `AssetsModule`, which is loaded by default. (If you are not using assets, you can disable this module by adding the configuration `play.modules.disabled += controllers.AssetsModule`.). The bindings there make `Assets` class injectable.

If you are using components traits to do compile-time dependency injection, you should mix in `controllers.AssetsComponents`. Then the controller will be available as `assets: Assets`. You do not need to construct the controller yourself.

### Using assets with configuration

For the most common case where you only have one place where assets are centrally located, you can use configuration to specify the location:

```
play.assets {
  path = "/public"
  urlPrefix = "/assets"
}
```

And use the `Assets.at` method with one parameter:

```scala
Assets.at(file: String)
```

Then in routes:

@[assets-configured-path](code/configured.assets.routes)

### Passing the assets path directly

The `Assets` controller also defines an `at` action with two parameters:

```scala
Assets.at(path: String, file: String)
```

The `path` parameter must be fixed and defines the directory managed by the action. The `file` parameter is usually dynamically extracted from the request path.

Here is the typical mapping of the `Assets` controller in your `conf/routes` file:

@[assets-wildcard](code/common.assets.routes)

Note that we define the `*file` dynamic part that will match the `.*` regular expression. So for example, if you send this request to the server:

```
GET /assets/javascripts/jquery.js
```

The router will invoke the `Assets.at` action with the following parameters:

```
controllers.Assets.at("/public", "javascripts/jquery.js")
```

To route to a single static file, both the path and file have to be specified:

@[assets-single-static-file](code/common.assets.routes)

## Reverse routing for public assets

As for any controller mapped in the routes file, a reverse controller is created in `controllers.routes.Assets`. You use this to reverse the URL needed to fetch a public resource. For example, from a template:

```html
<script src="@routes.Assets.at("javascripts/jquery.js")"></script>
```

In `DEV` mode this will by default produce the following result:

```html
<script src="/assets/javascripts/jquery.js"></script>
```

If your app is not running in `DEV` mode **and** a `jquery.min.js` or `jquery-min.js` file exists then by default the minified file will be used instead:

```html
<script src="/assets/javascripts/jquery.min.js"></script>
```

This makes debugging of JavaScript files easier during development. Of course this not only works for JavaScript files but for any file extension.
If you don't want Play to automatically resolve the `.min.*` or `-min.*` files, regardless of the mode your application is running in, you can set `play.assets.checkForMinified = false` in your `application.conf` (or to `true` to always resolve the min file, even in `DEV` mode).

Note that we don’t specify the first `folder` parameter when we reverse the route. This is because our routes file defines a single mapping for the `Assets.at` action, where the `folder` parameter is fixed. So it doesn't need to be specified.

However, if you define two mappings for the `Assets.at` action, like this:

@[assets-two-mappings](code/common.assets.routes)

You will then need to specify both parameters when using the reverse router:

```html
<script src="@routes.Assets.at("/public/javascripts", "jquery.js")"></script>
<img src="@routes.Assets.at("/public/images", "logo.png")" />
```

## Reverse routing and fingerprinting for public assets

[sbt-web](https://github.com/sbt/sbt-web) brings the notion of a highly configurable asset pipeline to Play e.g. in your build file:

```scala
pipelineStages := Seq(rjs, digest, gzip)
```

The above will order the RequireJs optimizer ([sbt-rjs](https://github.com/sbt/sbt-rjs)), the digester ([sbt-digest](https://github.com/sbt/sbt-digest)) and then compression ([sbt-gzip](https://github.com/sbt/sbt-gzip)). Unlike many sbt tasks, these tasks will execute in the order declared, one after the other.

In essence asset fingerprinting permits your static assets to be served with aggressive caching instructions to a browser. This will result in an improved experience for your users given that subsequent visits to your site will result in less assets requiring to be downloaded. Rails also describes the benefits of [asset fingerprinting](https://guides.rubyonrails.org/asset_pipeline.html#what-is-fingerprinting-and-why-should-i-care-questionmark).

The above declaration of `pipelineStages` and the requisite `addSbtPlugin` declarations in your `plugins.sbt` for the plugins you require are your start point. You must then declare to Play what assets are to be versioned.

There are two ways obtain the real path of a fingerprinted asset. The first way uses static state and supports the same style as normal reverse routing. It does so by looking up assets metadata that's set by a running Play application. The second way is to use configuration and inject an AssetsFinder to find your asset.

### Using reverse routing and static state

If you plan to use the reverse router with static state, the following routes file entry declares that all assets are to be versioned:

```scala
GET  /assets/*file  controllers.Assets.versioned(path="/public", file: Asset)
```

> **Note:** Make sure you indicate that `file` is an asset by writing `file: Asset`.

You then use the reverse router, for example within a `scala.html` view:

```html
<link rel="stylesheet" href="@routes.Assets.versioned("assets/css/app.css")">
```

The downside of this approach is that it requires special logic that converts the `Asset` from the path you passed in to the final minified path with a digest. It's also more difficult to unit test, since there's no component you can mock to define the path.

### Using configuration and AssetsFinder

You can also define your paths in configuration, and inject an `AssetsFinder` into your controller to get the final path. In your configuration set up the assets `path` (the directory containing assets) and the `urlPrefix` (the prefix to the URL in your application):

```
play.assets {
  path = "/public"
  urlPrefix = "/assets"
}
```

In your routes file you can define a route as follows:

@[assets-configured-path-versioned](code/configured.assets.routes)

(you should not use the `: Asset` type annotation here)

Then you can pass an `AssetsFinder` to your template and use that to get the final path:

```html
@(assetsFinder: AssetsFinder)

<link rel="stylesheet" href="@assetsFinder.path("assets/css/app.css")">
```

The advantage to this approach is that it requires no static state to set up. That means you can unit test your controllers and templates without a running application by simply passing an instance of `AssetsFinder`. That makes it simple to mock for a unit test by simply implementing the abstract methods that return `String`s.

Using the `AssetsFinder` approach also makes it easy to run multiple self-contained applications at once in the same classloader, since it uses no static state. This can also be helpful for testing.

The `AssetsFinder` interface also works in cases where fingerprinting is not used. It returns the original asset if a fingerprinted and/or minified asset cannot be found.

## Etag support

The `Assets` controller automatically manages **ETag** HTTP Headers. The ETag value is generated from the digest (if `sbt-digest` is being used in the asset pipeline) or otherwise the resource name and the file’s last modification date. If the resource file is embedded into a file, the JAR file’s last modification date is used.

When a web browser makes a request specifying this **Etag** then the server can respond with **304 NotModified**.

## Gzip support

If a resource with the same name but using a `.gz` suffix is found then the `Assets` controller will also serve the latter and add the following HTTP header:

```
Content-Encoding: gzip
```

Including the `sbt-gzip` plugin in your build and declaring its position in the `pipelineStages` is all that is required to generate gzip files.

## Additional `Cache-Control` directive

Using Etag is usually enough for the purposes of caching. However if you want to specify a custom `Cache-Control` header for a particular resource, you can specify it in your `application.conf` file. For example:

```
# Assets configuration
# ~~~~~
play.assets.cache."/public/stylesheets/bootstrap.min.css"="max-age=3600"
```

You can also use partial paths to specify a custom `Cache-Control` for all the assets that are under that path, for example:

```
# Assets configuration
# ~~~~~
play.assets.cache."/public/stylesheets/"="max-age=100"
play.assets.cache."/public/javascripts/"="max-age=200"
```

And Play will use `max-age=200` for all assets under `/public/javascripts` (like `/public/javascripts/main.js`) and `max-age=100` to all assets under `/public/stylesheets` (like `/public/stylesheets/main.css`).

### How the additional directives are applied

Play sorts the `Cache-Control` directives lexicographically and later from more specific to less specific. For example, given the following configuration: 

```
# Assets configuration
# ~~~~~
play.assets.cache."/public/stylesheets/"="max-age=101"
play.assets.cache."/public/stylesheets/layout/"="max-age=102"
play.assets.cache."/public/stylesheets/app/"="max-age=103"
play.assets.cache."/public/stylesheets/layout/main.css"="max-age=103"

play.assets.cache."/public/javascripts/"="max-age=201"
play.assets.cache."/public/javascripts/app/"="max-age=202"
play.assets.cache."/public/javascripts/app/main.js"="max-age=203"
```

The directives will be sorted and applied in the following order:

```
play.assets.cache."/public/javascripts/app/main.js"="max-age=203"
play.assets.cache."/public/javascripts/app/"="max-age=202"
play.assets.cache."/public/javascripts/"="max-age=201"

play.assets.cache."/public/stylesheets/app/"="max-age=103"
play.assets.cache."/public/stylesheets/layout/main.css"="max-age=103"
play.assets.cache."/public/stylesheets/layout/"="max-age=102"
play.assets.cache."/public/stylesheets/"="max-age=101"
```

> **Note:** a configuration like `play.assets.cache."/public/stylesheets"="max-age=101"` will match both `public/stylesheets.css` and `public/stylesheets/main.css`, so you may want to add a trailing `/` to better differentiate directories, for example `play.assets.cache."/public/stylesheets/"="max-age=101"`.

## Managed assets

Starting with Play 2.3 managed assets are processed by [sbt-web](https://github.com/sbt/sbt-web#sbt-web) based plugins. Prior to 2.3 Play bundled managed asset processing in the form of CoffeeScript, LESS, JavaScript linting (ClosureCompiler) and RequireJS optimization. The following sections describe sbt-web and how the equivalent 2.2 functionality can be achieved. Note though that Play is not limited to this asset processing technology as many plugins should become available to sbt-web over time. Please check-in with the [sbt-web](https://github.com/sbt/sbt-web#sbt-web) project to learn more about what plugins are available.

Many plugins use sbt-web's [js-engine plugin](https://github.com/sbt/sbt-js-engine). js-engine is able to execute plugins written to the Node API either within the JVM via the excellent [Trireme](https://github.com/apigee/trireme#trireme) project, or directly on [Node.js](https://nodejs.org/) for superior performance. Note that these tools are used during the development cycle only and have no involvement during the runtime execution of your Play application. If you have Node.js installed then you are encouraged to declare the following environment variable. For Unix, if `SBT_OPTS` has been defined elsewhere then you can:

```bash
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
```

The above declaration ensures that Node.js is used when executing any sbt-web plugin.

## Range requests support

`Assets` controller automatically supports part of [RFC 7233](https://tools.ietf.org/html/rfc7233) which defines how range requests and partial responses works. The `Assets` controller will delivery a `206 Partial Content` if a satisfiable `Range` header is present in the request. It will also returns a `Accept-Ranges: bytes` for all assets delivery.

> **Note:** Besides the fact that some parsing is done to better handle multiple ranges, `multipart/byteranges` is not fully supported yet.

You can also return `206 Partial Content` when delivering files without using the `Assets` controller:

### Scala version

@[range-request](code/assets/controllers/RangeRequestController.scala)

### Java version

@[range-request](code/assets/controllers/JavaRangeRequestController.java)

Both examples will delivery just part of the video file, according to the requested range.
