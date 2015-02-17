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

## Defining custom action annotations

You can also mark action composition with your own annotation, which must itself be annotated using `@With`:

@[verbose-annotation](code/javaguide/http/JavaActionsComposition.java)

You can then use your new annotation with an action method:

@[verbose-annotation-index](code/javaguide/http/JavaActionsComposition.java)

Your `Action` definition retrieves the annotation as configuration:

@[verbose-annotation-action](code/javaguide/http/JavaActionsComposition.java)

## Annotating controllers

You can also put any action composition annotation directly on the `Controller` class. In this case it will be applied to all action methods defined by this controller.

```java
@Authenticated
public class Admin extends Controller {
    
  …
    
}
```

## Passing objects from action to controller

You can pass an object from an action to a controller by utilizing the context args map.

@[security-action](code/javaguide/http/JavaActionsComposition.java)
