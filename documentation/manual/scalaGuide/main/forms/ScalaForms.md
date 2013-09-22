# Handling form submission

## Overview

Form handling and submission is an important part of any web application.  Play comes with features that make handling simple forms easy and complex forms possible.

Play's form handling approach is based around the concept of binding data.  When data comes in from a POST request, Play will look for formatted values and bind them to a [`Form`](api/scala/index.html#play.api.data.Form) object.  From there, Play can use the bound form to value a case class with data, call custom validations, and so on.

Typically forms are used directly from a `Controller` instance.  However, `Form` definitions do not have to match up exactly with case classes or models: they are purely for handling input and it is reasonable to use a distinct `Form` for a distinct POST.

## Form Basics

First, we'll go through the basics of form handling: defining a form, displaying it in an HTML page, having the user submit the form, and processing the result (or the errors).

To use form functionality, import the following packages into your class:

```
import play.api.data._
import play.api.data.Forms._
```

### Defining a form

The easiest way to handle a form submission is to define a `play.api.data.Form` structure:

@[loginForm-define](code/ScalaForms.scala)

This form can generate a `(String, String)` result value from `Map[String, String]` data:

@[loginForm-generate-map](code/ScalaForms.scala)

However, most of the time you'll use forms from within an Action.  An Action has a request available to it, and `Form` contains `bindFromRequest`, which will take a request as an implicit parameter.  If you define `implicit request`, then `bindFromRequest` will find it.

```scala
def submit = Action { implicit request =>
  val (email, password) = loginForm.bindFromRequest.get
}
```

There is a catch to using `get` here.  If the form binding did not happen correctly, then `get` will throw an exception.

### Defining constraints

Because we are using the `text` constraint, even if the binding happened correctly then both email and password could be empty.

There are other options besides `text` though.  A way to ensure that email and password have the appropriate values is to use the `email` and `nonEmptyText` constraints.

```scala
val loginForm = Form(
  tuple(
   "email" -> email,
   "password" -> nonEmptyText
  )
)
```

Using this form will result in a form with errors if the input to the form does not match the constraints.

```scala
val badData = Map("email" -> "notanemail", "password" -> "")
val boundForm = loginForm.bind(data)
boundForm.hasErrors must beTrue
```

The out of the box constraints are:

* `text` - maps to `scala.String`, optionally takes minLength and maxLength as parameters
* `nonEmptyText` - maps to `scala.String`, optionally takes minLength and maxLength
* `number` - maps to `scala.Int`, optionally takes min, max, and strict.
* `longNumber` - maps to `scala.Long`, optionally takes min, max, and strict.
* `bigDecimal` - takes precision and scale
* `date` - maps to `java.util.Date`, optionally takes pattern and timeZone.
* `email` - maps to `scala.String`, using an email regular expression.
* `boolean` - maps to `scala.Boolean`
* `checked` - maps to `scala.Boolean`

### Validating a form in an Action

Now that we have constraints, we can validate the form inside an action, and process the form with errors.

We do this using the `fold` method, which takes two functions: the first is called if the binding fails, and the second is called if the
binding succeeds.

@[loginForm-handling-failure](code/ScalaForms.scala)


```scala
def submit = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      { formWithErrors =>
         ... // return a result
      },
      { (email, password) =>
        ... // res
      })
    )
}
```

### Showing forms in a template

TODO


### Displaying errors in a template

TODO

```scala
boundForm.errors // returns all errors.
boundForm.globalErrors // returns errors without a key.
boundForm.error("email") // returns the first error bound to key
boundForm.errors("password") // return all errors bound to key.
```




## Intermediate Forms

### Constructing complex objects

A form can use functions to construct and deconstruct the value. So you can, for example, define a form that wraps an existing case class:

@[userForm-get](code/ScalaForms.scala)

> **Note:** The difference between using `tuple` and `mapping` is that when you are using `tuple` the construction and deconstruction functions don’t need to be specified (we know how to construct and deconstruct a tuple, right?). 
>
> The `mapping` method just lets you define your custom functions. When you want to construct and deconstruct a case class, you can just use its default `apply` and `unapply` functions, as they do exactly that!

Of course often the `Form` signature doesn’t match the case class exactly. Let’s use the example of a form that contains an additional checkbox field, used to accept terms of service. We don’t need to add this data to our `User` value. It’s just a dummy field that is used for form validation but which doesn’t carry any useful information once validated.

As we can define our own construction and deconstruction functions, it is easy to handle it:

@[userForm-verify](code/ScalaForms.scala)

> **Note:** The deconstruction function is used when we fill a form with an existing `User` value. This is useful if we want the load a user from the database and prepare a form to update it.

### Fill a form with initial default values

Sometimes you’ll want to populate a form with existing values, typically for editing data:

@[userForm-filled](code/ScalaForms.scala)

### Nested values

A form mapping can define nested values:

@[userForm-nested](code/ScalaForms.scala)

When you are using nested data this way, the form values sent by the browser must be named like `address.street`, `address.city`, etc.

### Repeated values

A form mapping can also define repeated values:

@[userForm-repeated](code/ScalaForms.scala)

When you are using repeated data like this, the form values sent by the browser must be named `emails[0]`, `emails[1]`, `emails[2]`, etc.

### Optional values

A form mapping can also define optional values:

@[userForm-optional](code/ScalaForms.scala)

### Defining ad-hoc constraints


You can also define ad-hoc constraints on the fields:

@[userForm-constraints-ad-hoc](code/ScalaForms.scala)


> **Note:** The email field will be ignored and set to `None` if the field `email` is missing in the request payload or if it contains a blank value.

### Ignored values

If you want a form to have a static value for a field:

@[userForm-static-value](code/ScalaForms.scala)

Now you can mix optional, nested and repeated mappings any way you want to create complex forms.

## Advanced Forms

### Dealing with Dynamic Forms

TODO

### Defining custom constraints

TODO

### Formatters


> **Next:** [[Using the form template helpers | ScalaFormHelpers]]




