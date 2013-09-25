<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Handling form submission

## Overview

Form handling and submission is an important part of any web application.  Play comes with features that make handling simple forms easy and complex forms possible.

Play's form handling approach is based around the concept of binding data.  When data comes in from a POST request, Play will look for formatted values and bind them to a [`Form`](api/scala/index.html#play.api.data.Form) object.  From there, Play can use the bound form to value a case class with data, call custom validations, and so on.

Typically forms are used directly from a `Controller` instance.  However, [`Form`](api/scala/index.html#play.api.data.Form) definitions do not have to match up exactly with case classes or models: they are purely for handling input and it is reasonable to use a distinct `Form` for a distinct POST.

## Imports

To use forms, import the following packages into your class:

```
import play.api.data._
import play.api.data.Forms._
```

## Form Basics

We'll go through the basics of form handling:

* defining a form,
* defining constraints in the form,
* validating the form in an action,
* displaying the form in a view template,
* and finally, processing the result (or errors) of the form in a view template.

### Defining a form

The easiest way to handle a form submission is to define a [`Form`](api/scala/index.html#play.api.data.Form) structure:

@[loginForm-define](code/ScalaForms.scala)

This form can generate a `(String, String)` result value from `Map[String, String]` data:

@[loginForm-generate-map](code/ScalaForms.scala)

However, most of the time you'll use forms from within an Action.  An Action has a request available to it, and [`Form`](api/scala/index.html#play.api.data.Form) contains `bindFromRequest`, which will take a request as an implicit parameter.  If you define `implicit request`, then `bindFromRequest` will find it.

```scala
def login = Action { implicit request =>
  val (email, password) = loginForm.bindFromRequest.get
}
```

There is a catch to using `get` here.  If the form binding did not happen correctly, then `get` will throw an exception.

### Defining constraints on the form

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

The out of the box constraints are defined on the [Form object](api/scala/index.html#play.api.data.Forms$):

* `text` - maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* `nonEmptyText` - maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* `number` - maps to `scala.Int`, optionally takes `min`, `max`, and `strict`.
* `longNumber` - maps to `scala.Long`, optionally takes `min`, `max`, and `strict`.
* `bigDecimal` - takes `precision` and `scale`.
* `date` - maps to `java.util.Date`, optionally takes `pattern` and `timeZone`.
* `email` - maps to `scala.String`, using an email regular expression.
* `boolean` - maps to `scala.Boolean`.
* `checked` - maps to `scala.Boolean`.

You also have the option of constructing your own custom validations using the [validation package](api/scala/index.html#play.api.data.validation.package).

### Validating a form in an Action

Now that we have constraints, we can validate the form inside an action, and process the form with errors.

We do this using the `fold` method, which takes two functions: the first is called if the binding fails, and the second is called if the binding succeeds.

@[loginForm-handling-failure](code/ScalaForms.scala)

Note that the success case, we're sending a `Redirect` with a route to `routes.Application.home` here instead of rendering a view template.  This pattern is called  [Redirect after POST](http://en.wikipedia.org/wiki/Post/Redirect/Get), and is an excellent way to prevent duplicate form submissions.

> NOTE: "Redirect after POST" is *required* when using `flashing` or other methods with [[flash scope|ScalaSessionFlash]], as new cookies will only be available after the redirected HTTP request.

### Showing forms in a view template

Once you have a form, then you need to make it available to the [[template engine|ScalaTemplates]].  You do this by including the form as a parameter to the view template.  For `login.scala.html`, the form parameter will look like this:

```scala
@(loginForm:Form[(String,String)])
```

The form's type here is `(String,String)` in this case, reflecting the `(email,password)` tuple we defined earlier.

Because login.scala.html needs a form passed in, you should pass the empty `loginForm` initially when rendering the login page:

```scala
def index = Action {
  views.html.login(loginForm)
}
```

The first thing is to be able to create the [form tag](api/scala/index.html#views.html.helper.form$). It is a simple view helper that sets the `action` and `method` tag parameters according to the reverse route you pass in:

@[form-tag](code/scalaguide/forms/scalaformhelper/views/login.scala.html)

You can find several input helpers in the [`views.html.helper`](api/scala/index.html#views.html.helper.package) package.

* [`inputText`](api/scala/index.html#views.html.helper.inputText$)
* [`inputPassword`](api/scala/index.html#views.html.helper.inputPassword$)
* [`inputRadioGroup`](api/scala/index.html#views.html.helper.inputRadioGroup$)
* [`inputDate`](api/scala/index.html#views.html.helper.inputDate$)
* [`inputFile`](api/scala/index.html#views.html.helper.inputFile$)
* [`select`](api/scala/index.html#views.html.helper.select$)
* [`textarea`](api/scala/index.html#views.html.helper.textarea$)
* [`checkbox`](api/scala/index.html#views.html.helper.checkbox$)

### Displaying results (or errors) in a view template

TODO

```scala
boundForm.errors // returns all errors.
boundForm.globalErrors // returns errors without a key.
boundForm.error("email") // returns the first error bound to key
boundForm.errors("password") // return all errors bound to key.
```

## Intermediate Forms

### Constructing complex objects

The [Forms object](api/scala/index.html#play.api.data.Forms$) has mapping, tuple, etc.

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

Now you have to generate as many inputs for the `emails` field as the form contains. Just use the `repeat` helper for that:

@[form-field-repeat](code/scalaguide/forms/scalaformhelper/views/register.scala.html)

The `min` parameter allows you to display a minimum number of fields even if the corresponding form data are empty.


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




