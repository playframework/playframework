# RequireJS

According to [RequireJS](http://requirejs.org/)' website 

> RequireJS is a JavaScript file and module loader. It is optimized for in-browser use, but it can be used in other JavaScript environments, like Rhino and Node. Using a modular script loader like RequireJS will improve the speed and quality of your code.

What this means in practice is that one can use [RequireJS](http://requirejs.org/) to modularize big javascript codebases. RequireJS achieves this by implementing a semi-standard API called [Asynchronous Module Definition](http://wiki.commonjs.org/wiki/Modules/AsynchronousDefinition) (other similar ideas include [CommonJS](http://www.commonjs.org/) ). Using AMD it's possible to resolve and load javascript modules, usually kept in separate files, at _client side_ while allowing server side _optimization_, that is, for production use, dependencies can be minified and combined. Therefore, RequireJs supports both client side and server side resolutions.

RequireJs support is enabled by default, so all you need to do is to drop javascript modules into ```public/javascripts``` and then bootstrap the module using one of the preferred RequireJS bootstraping techniques.


## Things to know about the implementation

* ```require.js``` is bundled with play, so users do not need to add it manually
* in dev mode dependencies resolved client side, closure compiler - without commonJS support - is run through the scripts for sanity check but no files are modified
* ```requireJs``` setting key in your build script should contain the list of modules you want to run through the optimizer (modules should be relative to ```app/assets/javascripts```) 
* empty ```requireJs``` key indicates that no optimization should take place
*  ```stage```, ```dist``` and ```start``` commands were changed to
run [RequireJS's optimizer](http://requirejs.org/docs/optimization.html) for configured moduled in ```app/assets/javascripts```. The minified and combined assets are stored in ```app/assets/javascripts-min```
* a new template tag ```@requireJs``` can be used  to switch between dev and prod mode seamlessly 
* by default a rhino based optimizer is used, the native, node version can be configured for performance via ```requireNativePath``` setting
* right now this is enabled only for javascript but we are looking into using it for css as well

## Example

create `app/assets/javascripts/main.js`:

```js
require(["helper/lib"],function(l) {
	var s = l.sum(4, 5);
	alert(s);
});
```

create `app/assets/javascripts/helper/lib.js`:

```js
define(function() {
    return {
         sum: function(a,b) {
    		return a + b;
        }
    }
});
```

create `app/views/index.scala.html`:

```html
@helper.requireJs(core = routes.Assets.at("javascripts/require.js").url, module = routes.Assets.at("javascripts/main").url)
```

In `project/Build.scala` add:

```
val main = play.Project(appName, appVersion, appDependencies).settings(
    	requireJs += "main.js"
    )	
```

After rendering the page in Dev mode you should see: ```9``` popping up in an alert

## When running stage, dist or start

your application's jar file should contain (```public/javascript/main.js```):

```js
define("helper/lib",[],function(){return{sum:function(e,t){return e+t}}}),require(["helper/lib"],function(e){var t=e.sum(5,4);alert(t)}),define("main",function(){})
```