# Action composition

This chapter introduces several ways of defining generic action functionality.

## Basic action composition

Let’s start with the simple example of a logging decorator: we want to log each call to this action.

The first way is not to define our own Action, but just to provide a helper method building a standard Action:

```scala
def LoggingAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
  Action { request =>
    Logger.info("Calling action")
    f(request)
  }
}
```

That you can use as:

```scala
def index = LoggingAction { request =>
  Ok("Hello World")
}
```

This is simple but it works only with the default `parse.anyContent` body parser as we don't have a way to specify our own body parser. We can of course define an additional helper method:

```scala
def LoggingAction[A](bp: BodyParser[A])(f: Request[A] => Result): Action[A] = {
  Action(bp) { request =>
    Logger.info("Calling action")
    f(request)
  }
}
```

And then:

```scala
def index = LoggingAction(parse.text) { request =>
  Ok("Hello World")
}
```

## Wrapping existing actions

Another way is to define our own `LoggingAction` that would be a wrapper over another `Action`:

```scala
case class Logging[A](action: Action[A]) extends Action[A] {
  
  def apply(request: Request[A]): Result = {
    Logger.info("Calling action")
    action(request)
  }
  
  lazy val parser = action.parser
}
```

Now you can use it to wrap any other action value:

```scala
def index = Logging { 
  Action { 
    Ok("Hello World")
  }
}
```

Note that it will just re-use the wrapped action body parser as is, so you can of course write:

```scala
def index = Logging { 
  Action(parse.text) { 
    Ok("Hello World")
  }
}
```

> Another way to write the same thing but without defining the `Logging` class, would be:
> 
> ```scala
> def Logging[A](action: Action[A]): Action[A] = {
>   Action(action.parser) { request =>
>     Logger.info("Calling action")
>     action(request)
>   }
> } 
> ```

The following example is wrapping an existing action to add session variable:

```scala
def addSessionVar[A](action: Action[A]) = Action(action.parser) { request =>
  action(request).withSession("foo" -> "bar")
}
``` 


## A more complicated example

Let’s look at the more complicated but common example of an authenticated action. The main problem is that we need to pass the authenticated user to the wrapped action.

```scala
def Authenticated(action: User => EssentialAction): EssentialAction = {
  
  // Let's define a helper function to retrieve a User
  def getUser(request: RequestHeader): Option[User] = {
    request.session.get("user").flatMap(u => User.find(u))
  }
  
  // Now let's define the new Action
  EssentialAction { request =>
    getUser(request).map(u => action(u)(request)).getOrElse {
      Done(Unauthorized)
    }
  }
  
}
```

You can use it like this:

```scala
def index = Authenticated { user =>
  Action { request =>
    Ok("Hello " + user.name)
  }
}
```

> **Note:** There is already an `Authenticated` action in `play.api.mvc.Security.Authenticated` with a more generic implementation than this example.

In the [[previous section | ScalaBodyParsers]] we said that an `Action[A]` was a `Request[A] => Result` function but this is not entirely true. Actually the `Action[A]` trait is defined as follows:

```scala
trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], Result])

trait Action[A] extends EssentialAction {
  def parser: BodyParser[A]
  def apply(request: Request[A]): Result
  def apply(headers: RequestHeader): Iteratee[Array[Byte], Result] = …
}
```

An `EssentialAction` is a function that takes the request headers and gives an `Iteratee` that will eventually parse the request body and produce a HTTP result. `Action[A]` implements `EssentialAction` as follow: it parses the request body using its body parser, gives the built `Request[A]` object to the action code and returns the action code’s result. An `Action[A]` can still be thought of as a `Request[A] => Result` function because it has an `apply(request: Request[A]): Result` method.

The `EssentialAction` trait is useful to compose actions with code that requires to read some information from the request headers before parsing the request body.

Our `Authenticated` implementation above tries to find a user id in the request session and calls the wrapped action with this user if found, otherwise it returns a `401 UNAUTHORIZED` status without even parsing the request body.

## Another way to create the Authenticated action

Let’s see how to write the previous example without wrapping the whole action:

```scala
def Authenticated(f: (User, Request[AnyContent]) => Result) = {
  Action { request =>
    val result = for {
      id <- request.session.get("user")
      user <- User.find(id)
    } yield f(user, request)
    result getOrElse Unauthorized
  }
}
```

To use this:

```scala
def index = Authenticated { (user, request) =>
   Ok("Hello " + user.name)
}
```

A problem here is that you can't mark the `request` parameter as `implicit` anymore. You can solve that using currying:

```scala
def Authenticated(f: User => Request[AnyContent] => Result) = {
  Action { request =>
    val result = for {
      id <- request.session.get("user")
      user <- User.find(id)
    } yield f(user)(request)
    result getOrElse Unauthorized
  }
}
```

Then you can do this:

```scala
def index = Authenticated { user => implicit request =>
   Ok("Hello " + user.name)
}
```

Another (probably simpler) way is to define our own subclass of `Request` as `AuthenticatedRequest` (so we are merging both parameters into a single parameter):

```scala
case class AuthenticatedRequest(
  user: User, private val request: Request[AnyContent]
) extends WrappedRequest(request)

def Authenticated(f: AuthenticatedRequest => Result) = {
  Action { request =>
    val result = for {
      id <- request.session.get("user")
      user <- User.find(id)
    } yield f(AuthenticatedRequest(user, request))
    result getOrElse Unauthorized
  }
}
```

And then:

```scala
def index = Authenticated { implicit request =>
   Ok("Hello " + request.user.name)
}
```

We can of course extend this last example and make it more generic by making it possible to specify a body parser:

```scala
case class AuthenticatedRequest[A](
  user: User, private val request: Request[A]
) extends WrappedRequest(request)

def Authenticated[A](p: BodyParser[A])(f: AuthenticatedRequest[A] => Result) = {
  Action(p) { request =>
    val result = for {
      id <- request.session.get("user")
      user <- User.find(id)
    } yield f(Authenticated(user, request))
    result getOrElse Unauthorized
  }
}

// Overloaded method to use the default body parser
import play.api.mvc.BodyParsers._
def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent]  = {
  Authenticated(parse.anyContent)(f)
}
```

> Play also provides a [[global filter API | ScalaHttpFilters]], which is useful for global cross cutting concerns.

> **Next:** [[Content negotiation | ScalaContentNegotiation]]
