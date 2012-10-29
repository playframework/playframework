> Available in 2.1

# RequireJS

According to [RequireJS](http://requirejs.org/)' website 

> RequireJS is a JavaScript file and module loader. It is optimized for in-browser use, but it can be used in other JavaScript environments, like Rhino and Node. Using a modular script loader like RequireJS will improve the speed and quality of your code.

What this means in practice is that one can use [RequireJS](http://requirejs.org/) to modularize big javascript codebases. RequireJS achieves this by implementing a semi-standard API called [Asynchronous Module Definition](http://wiki.commonjs.org/wiki/Modules/AsynchronousDefinition) (other similar ideas include [CommonJS](http://www.commonjs.org/) ). Using AMD it's possible to resolve and load javascript modules, usually kept in separate files, at _client side_ while allowing server side _optimization_, that is, for production use, dependencies can be minified and combined. Therefore, RequireJs supports both client side and server side resolutions.

RequireJs support is enabled by default, so all you need to do is to drop javascript modules into ```public/javascripts``` and then bootstrap the module using one of the preferred RequireJS bootstraping techniques.


# Things to know about the implementation
* ```require.js``` is bundled with play, so users do not need to add it manually
* in dev mode dependencies resolved client side, closure compiler, without commonJS support, is run through the scripts for sanity check but no files are modified
* in prod mode: stage, dist and start commands were changed to
run [RequireJS's optimizer](http://requirejs.org/docs/optimization.html) for each file in ```app/assets/javascripts``` this means files will be minified and combined for performance. Since the files (and references) will be the same (just optimized), no extra changes are necessary to switch between dev and prod mode
* by default a rhino based optimizer is used, the native, node version can be configured for performance via ```requireNativePath``` setting
* you can disable this feature via ```requireJsSupport := true``` setting
* right now this is enabled only for javascript but we are looking into using it for css as well

#Example
app/assets/javascripts/main.js:
```js
require(["helper/lib"],function(l) {
	var s = l.sum(4, 5);
	alert(s);
});
```

app/assets/javascripts/helper/lib.js:
```js
define(function() {
    return {
         sum: function(a,b) {
    		return a + b;
        }
    }
});
```

app/views/index.scala.html:

```html
<script type="text/javascript" data-main="public/javascripts/main" 
src="@controllers.routes.Assets.at("javascripts/require.js")"></script>
```

after rendering the page in Dev mode you should see: ```9``` popping up in an alert

## When running stage, dist or start
your application's jar file should contain (```public/javascript/main.js```):
```js
define("helper/lib",[],function(){return{sum:function(e,t){return e+t}}}),require(["helper/lib"],function(e){var t=e.sum(5,4);alert(t)}),define("main",function(){})
```