# Handling form submission

## Defining a form

The `play.api.data` package contains several helpers to handle HTTP form data submission and validation. The easiest way to handle a form submission is to define a `play.api.data.Form` structure:

```scala
import play.api.data._
import play.api.data.Forms._

val loginForm = Form(
  tuple(
    "email" -> text,
    "password" -> text
  )
)
```

This form can generate a `(String, String)` result value from `Map[String,String]` data:

```scala
val anyData = Map("email" -> "bob@gmail.com", "password" -> "secret")
val (user, password) = loginForm.bind(anyData).get
```

If you have a request available in the scope, you can bind directly to it from the request content:

```scala
val (user, password) = loginForm.bindFromRequest.get
```

## Constructing complex objects

A form can use functions to construct and deconstruct the value. So you can, for example, define a form that wraps an existing case class:

```scala
import play.api.data._
import play.api.data.Forms._

case class User(name: String, age: Int)

val userForm = Form(
  mapping(
    "name" -> text,
    "age" -> number
  )(User.apply)(User.unapply)
)

val anyData = Map("name" -> "bob", "age" -> "18")
val user: User = userForm.bind(anyData).get
```

> **Note:** The difference between using `tuple` and `mapping` is that when you are using `tuple` the construction and deconstruction functions don’t need to be specified (we know how to construct and deconstruct a tuple, right?). 
>
> The `mapping` method just let you define your custom functions. When you want to construct and deconstruct a case class, you can just use its default `apply` and `unapply` functions, as they do exactly that!

Of course often the `Form` signature doesn’t match the case class exactly. Let’s use the example a form that contains an additional checkbox field, used to accept terms of service. We don’t need to add this data to our `User` value. It’s just a dummy field that is used for form validation but which doesn’t carry any useful information once validated.

As we can define our own construction and deconstruction functions, it is easy to handle it:

```scala
val userForm = Form(
  mapping(
    "name" -> text,
    "age" -> number,
    "accept" -> checked("Please accept the terms and conditions")
  )((name, age, _) => User(name, age))
   ((user: User) => Some((user.name, user.age, false))
)
```

> **Note:** The deconstruction function is used when we fill a form with an existing `User` value. This is useful if we want the load a user from the database and prepare a form to update it.

## Defining constraints

For each mapping, you can also define additional validation constraints that will be checked during the binding phase:

```scala
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

case class User(name: String, age: Int)

val userForm = Form(
  mapping(
    "name" -> text.verifying(required),
    "age" -> number.verifying(min(0), max(100))
  )(User.apply)(User.unapply)
)
```

> **Note:** That can be also written:
>
> ```scala
> mapping(
>   "name" -> nonEmptyText,
>   "age" -> number(min=0, max=100)
> )
> ```
>
> This constructs the same mappings, with additional constraints.

You can also define ad-hoc constraints on the fields:

```scala
val loginForm = Form(
  tuple(
    "email" -> nonEmptyText,
    "password" -> text
  ) verifying("Invalid user name or password", fields => fields match { 
      case (e, p) => User.authenticate(e,p).isDefined 
  })
)
```

## Handling binding failure

If you can define constraints, then you need to be able to handle the binding errors. You can use the `fold` operation for this:

```scala
loginForm.bindFromRequest.fold(
  formWithErrors => // binding failure, you retrieve the form containing errors,
  value => // binding success, you get the actual value 
)
```

## Fill a form with initial default values

Sometimes you’ll want to populate a form with existing values, typically for editing data:

```scala
val filledForm = userForm.fill(User("Bob", 18))
```

## Nested values

A form mapping can define nested values:

```scala
case class User(name: String, address: Address)
case class Address(street: String, city: String)

val userForm = Form(
  mapping(
    "name" -> text,
    "address" -> mapping(
        "street" -> text,
        "city" -> text
    )(Address.apply)(Address.unapply)
  )(User.apply, User.unapply)
)
```

When you are using nested data this way, the form values sent by the browser must be named like `address.street`, `address.city`, etc.

## Repeated values

A form mapping can also define repeated values:

```scala
case class User(name: String, emails: List[String])

val userForm = Form(
  mapping(
    "name" -> text,
    "emails" -> list(text)
  )(User.apply, User.unapply)
)
```

When you are using repeated data like this, the form values sent by the browser must be named `emails[0]`, `emails[1]`, `emails[2]`, etc.

## Optional values

A form mapping can also define optional values:

```scala
case class User(name: String, email: Option[String])

val userForm = Form(
  mapping(
    "name" -> text,
    "email" -> optional(text)
  )(User.apply, User.unapply)
)
```

> **Note:** The email field will be ignored and set to `None` if the field `email` is missing in the request payload or if it contains a blank value.

## Ignored values

If you want a form to have a static value for a field:

```scala
case class User(id: Long, name: String, email: Option[String])

val userForm = Form(
  mapping(
    "id" -> ignored(1234),
    "name" -> text,
    "email" -> optional(text)
  )(User.apply, User.unapply)
)
```

Now you can mix optional, nested and repeated mappings any way you want to create complex forms.

> **Next:** [[Using the form template helpers | ScalaFormHelpers]]





