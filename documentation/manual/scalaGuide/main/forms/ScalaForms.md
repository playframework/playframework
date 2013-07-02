# Handling form submission

## Defining a form

The `play.api.data` package contains several helpers to handle HTTP form data submission and validation. The easiest way to handle a form submission is to define a `play.api.data.Form` structure:

@[loginForm-define](code/ScalaForms.scala)


This form can generate a `(String, String)` result value from `Map[String, String]` data:

@[loginForm-generate-map](code/ScalaForms.scala)


If you have a request available in the scope, you can bind directly to it from the request content:

@[loginForm-generate-request](code/ScalaForms.scala)

## Constructing complex objects

A form can use functions to construct and deconstruct the value. So you can, for example, define a form that wraps an existing case class:

@[userForm-get](code/ScalaForms.scala)

> **Note:** The difference between using `tuple` and `mapping` is that when you are using `tuple` the construction and deconstruction functions don’t need to be specified (we know how to construct and deconstruct a tuple, right?). 
>
> The `mapping` method just lets you define your custom functions. When you want to construct and deconstruct a case class, you can just use its default `apply` and `unapply` functions, as they do exactly that!

Of course often the `Form` signature doesn’t match the case class exactly. Let’s use the example of a form that contains an additional checkbox field, used to accept terms of service. We don’t need to add this data to our `User` value. It’s just a dummy field that is used for form validation but which doesn’t carry any useful information once validated.

As we can define our own construction and deconstruction functions, it is easy to handle it:

@[userForm-verify](code/ScalaForms.scala)

> **Note:** The deconstruction function is used when we fill a form with an existing `User` value. This is useful if we want the load a user from the database and prepare a form to update it.

## Defining constraints

For each mapping, you can also define additional validation constraints that will be checked during the binding phase:

@[userForm-constraints](code/ScalaForms.scala)

> **Note:** That can be also written:
>
> @[userForm-constraints-2](code/ScalaForms.scala)

>
> This constructs the same mappings, with additional constraints.

You can also define ad-hoc constraints on the fields:

@[userForm-constraints-ad-hoc](code/ScalaForms.scala)


## Handling binding failure

If you can define constraints, then you need to be able to handle the binding errors. You can use the `fold` operation for this:

@[loginForm-handling-failure](code/ScalaForms.scala)

## Fill a form with initial default values

Sometimes you’ll want to populate a form with existing values, typically for editing data:

@[userForm-filled](code/ScalaForms.scala)


## Nested values

A form mapping can define nested values:

@[userForm-nested](code/ScalaForms.scala)

When you are using nested data this way, the form values sent by the browser must be named like `address.street`, `address.city`, etc.

## Repeated values

A form mapping can also define repeated values:

@[userForm-repeated](code/ScalaForms.scala)

When you are using repeated data like this, the form values sent by the browser must be named `emails[0]`, `emails[1]`, `emails[2]`, etc.

## Optional values

A form mapping can also define optional values:

@[userForm-optional](code/ScalaForms.scala)


> **Note:** The email field will be ignored and set to `None` if the field `email` is missing in the request payload or if it contains a blank value.

## Ignored values

If you want a form to have a static value for a field:

@[userForm-static-value](code/ScalaForms.scala)

Now you can mix optional, nested and repeated mappings any way you want to create complex forms.

> **Next:** [[Using the form template helpers | ScalaFormHelpers]]




