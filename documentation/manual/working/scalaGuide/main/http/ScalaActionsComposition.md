<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Action composition

This chapter introduces several ways of defining generic action functionality.

## Custom action builders

We saw [[previously|ScalaActions]] that there are multiple ways to declare an action - with a request parameter, without a request parameter, with a body parser etc.  In fact there are more than this, as we'll see in the chapter on [[asynchronous programming|ScalaAsync]].

These methods for building actions are actually all defined by a trait called [`ActionBuilder`](api/scala/index.html#play.api.mvc.ActionBuilder), and the [`Action`](api/scala/index.html#play.api.mvc.Action$) object that we use to declare our actions is just an instance of this trait.  By implementing your own `ActionBuilder`, you can declare reusable action stacks, that can then be used to build actions.

Let’s start with the simple example of a logging decorator, we want to log each call to this action.

The first way is to implement this functionality in the `invokeBlock` method, which is called for every action built by the `ActionBuilder`:

@[basic-logging](code/ScalaActionsComposition.scala)

Now we can use it the same way we use `Action`:

@[basic-logging-index](code/ScalaActionsComposition.scala)
 
Since `ActionBuilder` provides all the different methods of building actions, this also works with, for example, declaring a custom body parser:

@[basic-logging-parse](code/ScalaActionsComposition.scala)


### Composing actions

In most applications, we will want to have multiple action builders, some that do different types of authentication, some that provide different types of generic functionality, etc.  In which case, we won't want to rewrite our logging action code for each type of action builder, we will want to define it in a reuseable way.

Reusable action code can be implemented by wrapping actions:

@[actions-class-wrapping](code/ScalaActionsComposition.scala)

We can also use the `Action` action builder to build actions without defining our own action class:

@[actions-def-wrapping](code/ScalaActionsComposition.scala)

Actions can be mixed in to action builders using the `composeAction` method:

@[actions-wrapping-builder](code/ScalaActionsComposition.scala)

Now the builder can be used in the same way as before:

@[actions-wrapping-index](code/ScalaActionsComposition.scala)

We can also mix in wrapping actions without the action builder:

@[actions-wrapping-direct](code/ScalaActionsComposition.scala)

### More complicated actions

So far we've only shown actions that don't impact the request at all.  Of course, we can also read and modify the incoming request object:

@[modify-request](code/ScalaActionsComposition.scala)

> **Note:** Play already has built in support for X-Forwarded-For headers.

We could block the request:

@[block-request](code/ScalaActionsComposition.scala)

And finally we can also modify the returned result:

@[modify-result](code/ScalaActionsComposition.scala)

## Different request types

While action composition allows you to perform additional processing at the HTTP request and response level, often you want to build pipelines of data transformations that add context to or perform validation on the request itself.  `ActionFunction` can be thought of as a function on the request, parameterized over both the input request type and the output type passed on to the next layer.  Each action function may represent modular processing such as authentication, database lookups for objects, permission checks, or other operations that you wish to compose and reuse across actions.

There are a few pre-defined traits implementing `ActionFunction` that are useful for different types of processing:

* [`ActionTransformer`](api/scala/index.html#play.api.mvc.ActionTransformer) can change the request, for example by adding additional information.
* [`ActionFilter`](api/scala/index.html#play.api.mvc.ActionFilter) can selectively intercept requests, for example to produce errors, without changing the request value.
* [`ActionRefiner`](api/scala/index.html#play.api.mvc.ActionRefiner) is the general case of both of the above.
* [`ActionBuilder`](api/scala/index.html#play.api.mvc.ActionBuilder) is the special case of functions that take `Request` as input, and thus can build actions.

You can also define your own arbitrary `ActionFunction` by implementing the `invokeBlock` method.  Often it is convenient to make the input and output types instances of `Request` (using `WrappedRequest`), but this is not strictly necessary.

### Authentication

One of the most common use cases for action functions is authentication.  We can easily implement our own authentication action transformer that determines the user from the original request and adds it to a new `UserRequest`.  Note that this is also an `ActionBuilder` because it takes a simple `Request` as input:

@[authenticated-action-builder](code/ScalaActionsComposition.scala)

Play also provides a built in authentication action builder.  Information on this and how to use it can be found [here](api/scala/index.html#play.api.mvc.Security$$AuthenticatedBuilder$).

> **Note:** The built in authentication action builder is just a convenience helper to minimise the code necessary to implement authentication for simple cases, its implementation is very similar to the example above.
>
> If you have more complex requirements than can be met by the built in authentication action, then implementing your own is not only simple, it is recommended.

### Adding information to requests

Now let's consider a REST API that works with objects of type `Item`.  There may be many routes under the `/item/:itemId` path, and each of these need to look up the item.  In this case, it may be useful to put this logic into an action function.

First of all, we'll create a request object that adds an `Item` to our `UserRequest`:

@[request-with-item](code/ScalaActionsComposition.scala)

Now we'll create an action refiner that looks up that item and returns `Either` an error (`Left`) or a new `ItemRequest` (`Right`).  Note that this action refiner is defined inside a method that takes the id of the item:

@[item-action-builder](code/ScalaActionsComposition.scala)

### Validating requests

Finally, we may want an action function that validates whether a request should continue.  For example, perhaps we want to check whether the user from `UserAction` has permission to access the item from `ItemAction`, and if not return an error:

@[permission-check-action](code/ScalaActionsComposition.scala)

### Putting it all together

Now we can chain these action functions together (starting with an `ActionBuilder`) using `andThen` to create an action:

@[item-action-use](code/ScalaActionsComposition.scala)


Play also provides a [[global filter API | ScalaHttpFilters]], which is useful for global cross cutting concerns.
