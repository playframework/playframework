<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Action composition

This chapter introduces several ways to define generic action functionality.

## Reminder about actions

Previously, we said that an action is a Java method that returns a `play.mvc.Result` value. Actually, Play manages internally actions as functions. An action provided by the Java API is an instance of [`play.mvc.Action`](api/java/play/mvc/Action.html). Play builds a root action for you that just calls the proper action method. This allows for more complicated action composition.

## Composing actions

Here is the definition of the `VerboseAction`:

@[verbose-action](code/javaguide/http/JavaActionsComposition.java)

You can compose the code provided by the action method with another `play.mvc.Action`, using the [`@With`](api/java/play/mvc/With.html) annotation:

@[verbose-index](code/javaguide/http/JavaActionsComposition.java)

At one point you need to delegate to the wrapped action using `delegate.call(...)`.

You also mix several actions by using custom action annotations:

@[authenticated-cached-index](code/javaguide/http/JavaActionsComposition.java)

> **Note:**  Every request **must** be served by a distinct instance of your `play.mvc.Action`. If a singleton pattern is used, requests will be routed incorrectly during multiple request scenarios. For example, if you are using Spring as a DI container for Play, you need to make sure that your action beans are prototype scoped.

> **Note:**  [`play.mvc.Security.Authenticated`](api/java/play/mvc/Security.Authenticated.html) and [`play.cache.Cached`](api/java/play/cache/Cached.html) annotations and the corresponding predefined Actions are shipped with Play. See the relevant API documentation for more information.

## Action annotations and WebSocket action methods

By default, action composition is not applied when handling `WebSocket`s. A guide how to enable action composition, including an example, can be found in the [[WebSockets documentation|JavaWebSockets#WebSockets-and-Action-composition]].

## Defining custom action annotations

You can also mark action composition with your own annotation, which must itself be annotated using `@With`:

@[verbose-annotation](code/javaguide/http/JavaActionsComposition.java)

Your `Action` definition retrieves the annotation as configuration:

@[verbose-annotation-action](code/javaguide/http/JavaActionsComposition.java)

You can then use your new annotation with an action method:

@[verbose-annotation-index](code/javaguide/http/JavaActionsComposition.java)

## Annotating controllers

You can also put any action composition annotation directly on the `Controller` class. In this case it will be applied to all action methods defined by this controller.

@[annotated-controller](code/javaguide/http/JavaActionsComposition.java)

> **Note:** If you want the action composition annotation(s) put on a `Controller` class to be executed before the one(s) put on action methods set `play.http.actionComposition.controllerAnnotationsFirst = true` in `application.conf`. However, be aware that if you use a third party module in your project it may rely on a certain execution order of its annotations.

## Passing objects from action to controller

You can pass an object from an action to a controller by utilizing request attributes.

@[pass-arg-action](code/javaguide/http/JavaActionsComposition.java)

Then in an action you can get the request attribute like this:

@[pass-arg-action-index](code/javaguide/http/JavaActionsComposition.java)

## Debugging the action composition order

To see in which order the actions of the action composition chain will be executed, please add the following to `logback.xml`:

```
<logger name="play.mvc.Action" level="DEBUG" />
```

You will now see the whole action composition chain with the according annotations (and their associated method/controller) in the logs:

```
[debug] play.mvc.Action - ### Start of action order
[debug] play.mvc.Action - 1. ...
[debug] play.mvc.Action - 2. ...
[debug] play.mvc.Action - ### End of action order
```

## Action composition in interaction with body parsing

By default [[body parsing|JavaBodyParsers]] takes place before action composition happens, meaning you are able to access the already parsed request body inside every action's `call(...)` method via `request.body()`. However, there are use cases where it makes sense to defer body parsing _after_ action composition took place. For example:

- When you want to pass request specific information to the body parser via [[request attributes|Highlights26#Request-attributes]]. E.g. user dependent maximum file upload size or user dependent credentials for a webservice or object storage where the body parser should redirect an upload to.
- When using action composition for (granular) [[authorization|ModuleDirectory#Authentication-(Login-&-Registration)-and-Authorization-(Restricted-Access)]] you may not want to even parse the request body and cancel the request early if permission checks fail.

Of course, when deferring body parsing, the request body won't be parsed yet inside a `call(...)` method and therefore `request.body()` will return `null`.

You can enable deferred body parsing globally in `conf/application.conf`:

```
play.server.deferBodyParsing = true
```

Just be aware that, like all `play.server.*` config keys, this config won't be picked up by Play when running in DEV mode, but only in PROD mode. To set this config in DEV mode you have to set it in `build.sbt`:

```
PlayKeys.devSettings += "play.server.deferBodyParsing" -> "true"
```

Instead of enabling deferred body parsing globally, you can enable it just for specific routes by using the [[routes modifier|JavaRouting#The-routes-file-syntax]] `deferBodyParsing`:

```
+ deferBodyParsing
POST    /      controllers.HomeController.uploadFileToS3(request: Request)
```

The opposite is true as well. If you globally enable deferred body parsing you can disable it for specific routes by using the [[routes modifier|JavaRouting#The-routes-file-syntax]] `dontDeferBodyParsing`:

```
+ dontDeferBodyParsing
POST    /      controllers.HomeController.processUpload(request: Request)
```

## Using Dependency Injection

You can use [[runtime Dependency Injection|JavaDependencyInjection]] or [[compile-time Dependency Injection|JavaCompileTimeDependencyInjection]]together with action composition. 

### Runtime Dependency Injection

For example, if you want to define your own result cache solution, first define the annotation:

@[action-composition-dependency-injection-annotation](code/javaguide/http/JavaActionsComposition.java)

And then you can define your action with the dependencies injected:

@[action-composition-dependency-injection](code/javaguide/http/JavaActionsComposition.java)

> **Note:** As stated above, every request **must** be served by a distinct instance of your `play.mvc.Action` and you **must not** annotate your action as a `@Singleton`.

### Compile-time Dependency Injection

When using [[compile-time Dependency Injection|JavaCompileTimeDependencyInjection]], you need to manually add your `Action` supplier to [`JavaHandlerComponents`](api/scala/play/core/j/JavaHandlerComponents.html). You do that by overriding method `javaHandlerComponents` in [`BuiltInComponents`](api/java/play/BuiltInComponents.html):

@[action-composition-compile-time-di](code/javaguide/http/JavaActionsComposition.java)

> **Note:** As stated above, every request **must** be served by a distinct instance of your `play.mvc.Action` and that is why you add a `java.util.function.Supplier<Action>` instead of the instance itself. Of course, you can have a `Supplier` returning the same instance every time, but this is not encouraged.
