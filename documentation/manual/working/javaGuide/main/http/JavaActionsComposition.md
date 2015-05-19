<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Action composition

This chapter introduces several ways to define generic action functionality.

## Reminder about actions

Previously, we said that an action is a Java method that returns a `play.mvc.Result` value. Actually, Play manages internally actions as functions. Because Java doesn't yet support first class functions, an action provided by the Java API is an instance of [`play.mvc.Action`](api/java/play/mvc/Action.html):

```java
public abstract class Action {
  public abstract Promise<Result> call(Context ctx) throws Throwable;
}
```

Play builds a root action for you that just calls the proper action method. This allows for more complicated action composition.

## Composing actions

Here is the definition of the `VerboseAction`:

@[verbose-action](code/javaguide/http/JavaActionsComposition.java)

You can compose the code provided by the action method with another `play.mvc.Action`, using the `@With` annotation:

@[verbose-index](code/javaguide/http/JavaActionsComposition.java)

At one point you need to delegate to the wrapped action using `delegate.call(...)`.

You also mix several actions by using custom action annotations:

@[authenticated-cached-index](code/javaguide/http/JavaActionsComposition.java)

> **Note:**  ```play.mvc.Security.Authenticated``` and ```play.cache.Cached``` annotations and the corresponding predefined Actions are shipped with Play. See the relevant API documentation for more information.

> **Note:**  Every request must be served by a distinct instance of your `play.mvc.Action`. If a singleton pattern is used, requests will be routed incorrectly during multiple request scenarios. For example, if you are using Spring as a DI container for Play, you need to make sure that your action beans are prototype scoped.

## Defining custom action annotations

You can also mark action composition with your own annotation, which must itself be annotated using `@With`:

@[verbose-annotation](code/javaguide/http/JavaActionsComposition.java)

Your `Action` definition retrieves the annotation as configuration:

@[verbose-annotation-action](code/javaguide/http/JavaActionsComposition.java)

You can then use your new annotation with an action method:

@[verbose-annotation-index](code/javaguide/http/JavaActionsComposition.java)

## Annotating controllers

You can also put any action composition annotation directly on the `Controller` class. In this case it will be applied to all action methods defined by this controller.

```java
@Authenticated
public class Admin extends Controller {
    
  …
    
}
```

> **Note:** If you want the action composition annotation(s) put on a ```Controller``` class to be executed before the one(s) put on action methods set ```play.http.actionComposition.controllerAnnotationsFirst = true``` in ```application.conf```. However, be aware that if you use a third party module in your project it may rely on a certain execution order of its annotations.

## Passing objects from action to controller

You can pass an object from an action to a controller by utilizing the context args map.

@[pass-arg-action](code/javaguide/http/JavaActionsComposition.java)

Then in an action you can get the arg like this:

@[pass-arg-action-index](code/javaguide/http/JavaActionsComposition.java)
