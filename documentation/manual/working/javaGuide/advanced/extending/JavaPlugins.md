<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Writing Plugins

In the context of the Play runtime, a plugin is a class that is able to plug into the Play lifecycle, and also allows sharing components in a non static way in your application.

Not every library that adds functionality to Play is or needs to be a plugin in this context - a library that provides a custom filter for example does not need to be a plugin.

Similarly, plugins don't necessarily imply that they are reusable between applications, it is often very useful to implement a plugin locally within an application, in order to hook into the Play lifecycle and share components in your code.

## Implementing plugins

Implementing a plugin requires two steps.  The first is to implement the `play.Plugin` interface:

@[code](code/javaguide/advanced/extending/MyPlugin.java)

The next step is to register this with Play.  This can be done by creating a file called `play.plugins` and placing it in the root of the classloader.  In a typical Play app, this means putting it in the `conf` folder:

```
2000:plugins.MyPlugin
```

Each line in the `play.plugins` file contains a number followed by the fully qualified name of the plugin to load.  The number is used to control lifecycle ordering, lower numbers will be started first and stopped last.  Multiple plugins can be declared in the one file, and any lines started with `#` are treated as comments.

Choosing the right number for ordering for a plugin is important, it needs to fit in appropriate according to what other plugins it depends on.  The plugins that Play uses use the following ordering numbers:

* *100* - Utilities that have no dependencies, such as the messages plugin
* *200* - Database connection pools
* *300-500* - Plugins that depend on the database, such as JPA, ebean and evolutions
* *600* - The Play cache plugin
* *700* - The WS plugin
* *1000* - The Akka plugin
* *10000* - The Global plugin, which invokes the `Global.onStart` and `Global.onStop` methods.  This plugin is intended to execute last.

## Accessing plugins

Plugins can be accessed via the `plugin` method on `play.Application`:

@[access-plugin](code/javaguide/advanced/extending/JavaPlugins.java)

## Actor example

A common use case for using plugins is to create and share actors around the application.  This can be done by implementing an actors plugin:

@[code](code/javaguide/advanced/extending/Actors.java)

Note the static methods that allow easy access to the `ActorRef` for each actor, instead of code having to use the plugins API directly.

The plugin can then be registered in `play.plugins`:

```
1100:actors.Actors
```

The reason `1100` was chosen for the ordering was because this plugin depends on the Akka plugin, and so must start after that.
