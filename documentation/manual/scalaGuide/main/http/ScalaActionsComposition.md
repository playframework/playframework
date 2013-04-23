# Action composition

This chapter introduces several ways of defining generic action functionality.

## Basic action composition

Let’s start with the simple example of a logging decorator: we want to log each call to this action.

The first way is not to define our own Action, but just to provide a helper method building a standard Action:

@[basic-logging](code/ScalaActionsComposition.scala)
 

That you can use as:

@[basic-logging-index](code/ScalaActionsComposition.scala)
 

This is simple but it works only with the default `parse.anyContent` body parser as we don't have a way to specify our own body parser. We can of course define an additional helper method:

@[basic-logging-parse](code/ScalaActionsComposition.scala)
 

And then:

@[basic-logging-parse-index](code/ScalaActionsComposition.scala)
 

## Wrapping existing actions

Another way is to define our own `LoggingAction` that would be a wrapper over another `Action`:

@[actions-class-wrapping](code/ScalaActionsComposition.scala)
 

Now you can use it to wrap any other action value:

@[actions-wrapping-index](code/ScalaActionsComposition.scala)
 

Note that it will just re-use the wrapped action body parser as is, so you can of course write:

@[actions-wrapping-index-parse](code/ScalaActionsComposition.scala) 

> Another way to write the same thing but without defining the `Logging` class, would be:
> 
> @[actions-def-wrapping](code/ScalaActionsComposition.scala)
>


The following example is wrapping an existing action to add session variable:

@[actions-def-addSessionVar](code/ScalaActionsComposition.scala)



## A more complicated example

Let’s look at the more complicated but common example of an authenticated action. The main problem is that we need to pass the authenticated user to the wrapped action.

@[authenticated-essentialaction](code/ScalaActionsComposition.scala)
 

You can use it like this:

@[authenticated-essentialaction-index](code/ScalaActionsComposition.scala)
  

> **Note:** There is already an `Authenticated` action in `play.api.mvc.Security.Authenticated` with a more generic implementation than this example.

In the [[previous section | ScalaBodyParsers]] we said that an `Action[A]` was a `Request[A] => Result` function but this is not entirely true. Actually the `Action[A]` trait is defined as follows:

@[Source-Code-Action](code/ScalaActionsComposition.scala)
  

An `EssentialAction` is a function that takes the request headers and gives an `Iteratee` that will eventually parse the request body and produce a HTTP result. `Action[A]` implements `EssentialAction` as follow: it parses the request body using its body parser, gives the built `Request[A]` object to the action code and returns the action code’s result. An `Action[A]` can still be thought of as a `Request[A] => Result` function because it has an `apply(request: Request[A]): Result` method.

The `EssentialAction` trait is useful to compose actions with code that requires to read some information from the request headers before parsing the request body.

Our `Authenticated` implementation above tries to find a user id in the request session and calls the wrapped action with this user if found, otherwise it returns a `401 UNAUTHORIZED` status without even parsing the request body.

## Another way to create the Authenticated action

Let’s see how to write the previous example without wrapping the whole action:

@[authenticated-action-param](code/ScalaActionsComposition.scala)


To use this:

@[authenticated-action-param-index](code/ScalaActionsComposition.scala)
  

A problem here is that you can't mark the `request` parameter as `implicit` anymore. You can solve that using currying:

@[authenticated-action-currying](code/ScalaActionsComposition.scala)
 

Then you can do this:

@[authenticated-action-currying-index](code/ScalaActionsComposition.scala)
 

Another (probably simpler) way is to define our own subclass of `Request` as `AuthenticatedRequest` (so we are merging both parameters into a single parameter):

@[authenticated-request](code/ScalaActionsComposition.scala)
  

And then:

@[authenticated-request-index](code/ScalaActionsComposition.scala)
   

We can of course extend this last example and make it more generic by making it possible to specify a body parser:

@[authenticated-request-parser](code/ScalaActionsComposition.scala)
  

> Play also provides a [[global filter API | ScalaHttpFilters]], which is useful for global cross cutting concerns.

> **Next:** [[Content negotiation | ScalaContentNegotiation]]
