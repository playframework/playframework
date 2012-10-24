# Writing Plugins

Play 2.0 comes with a few plugins predefined for all applications, these plugins are the following: 

* ```DBPlugin``` -> providing a JDBC datasource
* ```EvolutionPlugin``` -> provides migration  _(only available if db was configured)_
* ```EbeanPlugin``` -> provides Ebean support _(only available if db was configured)_
* ```MessagesPlugin``` - > provides i18n support
* ```BasicCachePlugin``` -> provides in-memory caching
* ```GlobalPlugin``` -> executes application's settings

However, one can easily add a new plugin to an application.

1. first step is to implement play.api.Plugin trait which has three methods: onStart, onStop and enabled - [for example](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/cache/Cache.scala))
2. this plugin should be available in the application either through pulling in it from a maven repository and referencing it
as an app dependency or the plugin code can be part of a play application
3. you can use it directly like ```app.plugin[MyPlugin].map(_.api.mymethod).getOrElse(throwMyerror)``` (where ```app``` is  a reference to the current application which can be obtain by importing play.api.Play.current) however, it's recommended to wrap it for convenience (for example, see [this](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/cache/Cache.scala))
4. in your app create a file: ``conf/play.plugins``` and add a reference to your plugin, just like this ```5000:com.example.MyPlugin```


_the number represents the plugin loading order, by setting it to > 1000 we can make sure it's loaded after the global plugins_

_Tip: If you are a scala developer but you want to share your plugin with java developers, you will need make sure your API is wrapped for Java users (see [this](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/cache/Cache.scala) and [this](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/java/play/cache/Cache.java) for an example)_

