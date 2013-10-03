<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Handling form submission

## Overview

Form handling and submission is an important part of any web application.  Play comes with features that make handling simple forms easy and complex forms possible.

Play's form handling approach is based around the concept of binding data.  When data comes in from a POST request, Play will look for formatted values and bind them to a [`Form`](api/scala/index.html#play.api.data.Form) object.  From there, Play can use the bound form to value a case class with data, call custom validations, and so on.

Typically forms are used directly from a `Controller` instance.  However, [`Form`](api/scala/index.html#play.api.data.Form) definitions do not have to match up exactly with case classes or models: they are purely for handling input and it is reasonable to use a distinct `Form` for a distinct POST.

## Imports

To use forms, import the following packages into your class:

@[form-imports](code/ScalaForms.scala)

## Form Basics

We'll go through the basics of form handling:

* defining a form,
* defining constraints in the form,
* validating the form in an action,
* displaying the form in a view template,
* and finally, processing the result (or errors) of the form in a view template.

The end result will look something like this:

[[images/lifecycle.png]]

### Defining a form

First, define a case class which contains the elements you want in the form.  Here we want to capture the name and age of a user, so we create a UserData object:

@[userData-define](code/ScalaForms.scala)

Now that we have a case class, the next step is to define a [`Form`](api/scala/index.html#play.api.data.Form) structure:

@[userForm-define](code/ScalaForms.scala)


The [Forms](api/scala/index.html#play.api.data.Forms$) object defines the [`mapping`](api/scala/index.html#play.api.data.Forms$@mapping%5BR%2CA1%5D\(\(String%2CMapping%5BA1%5D\)\)\(\(A1\)%E2%87%92R\)\(\(R\)%E2%87%92Option%5BA1%5D\)%3AMapping%5BR%5D) method. This method takes the names and constraints of the form, and also takes two functions: an `apply` function and an `unapply` function.  Because UserData is a case class, we can plug its apply and unapply methods  directly into the mapping method.

> Case classes will only map up to 22 different fields.  If you have more than 22 fields in your form, you should break down your forms using lists or nested values.

A form will create UserData instance with the bound values when given a Map:

@[userForm-generate-map](code/ScalaForms.scala)

But most of the time you'll use forms from within an Action, with data provided from the request. [`Form`](api/scala/index.html#play.api.data.Form) contains [`bindFromRequest`](api/scala/index.html#play.api.data.Form@bindFromRequest\(\)\(Request%5B_%5D\)%3AForm%5BT%5D), which will take a request as an implicit parameter.  If you define `implicit request`, then `bindFromRequest` will find it.

@[userForm-generate-request](code/ScalaForms.scala)

> There is a catch to using `get` here.  If the form cannot bind to the data, then `get` will throw an exception:

> ```scala
scala> userForm.bind(Map("name" -> "bob", "age" -> "notanumber")).get
java.util.NoSuchElementException: None.get
    at scala.None$.get(Option.scala:313)
    at scala.None$.get(Option.scala:311)
    at play.api.data.Form.get(Form.scala:220)
```

> We'll show how to deal with invalid input in the next few sections.

### Defining constraints on the form

Because we are using the `text` constraint, even if the binding happened correctly then `name` could be empty.

There are other options besides `text` though.  A way to ensure that `name` has the appropriate value is to use the `nonEmptyText` constraint.

@[userForm-constraints-2](code/ScalaForms.scala)

Using this form will result in a form with errors if the input to the form does not match the constraints.

XXX Replace with code snippet

```scala
val badData = Map("bob" -> "", "age" -> "25")
val boundForm = loginForm.bind(data)
boundForm.hasErrors must beTrue
```

The out of the box constraints are defined on the [Form object](api/scala/index.html#play.api.data.Forms$):

* [`text`](api/scala/index.html#play.api.data.Forms$@text%3AMapping%5BString%5D): maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* [`nonEmptyText`](api/scala/index.html#play.api.data.Forms$@nonEmptyText%3AMapping%5BString%5D): maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* [`number`](api/scala/index.html#play.api.data.Forms$@number%3AMapping%5BInt%5D): maps to `scala.Int`, optionally takes `min`, `max`, and `strict`.
* [`longNumber`](api/scala/index.html#play.api.data.Forms$@longNumber%3AMapping%5BLong%5D): maps to `scala.Long`, optionally takes `min`, `max`, and `strict`.
* [`bigDecimal`](api/scala/index.html#play.api.data.Forms$@bigDecimal%3AMapping%5BBigDecimal%5D): takes `precision` and `scale`.
* [`date`](api/scala/index.html#play.api.data.Forms$@date%3AMapping%5BDate%5D): maps to `java.util.Date`, optionally takes `pattern` and `timeZone`.
* [`email`](api/scala/index.html#play.api.data.Forms$@email%3AMapping%5BString%5D): maps to `scala.String`, using an email regular expression.
* [`boolean`](api/scala/index.html#play.api.data.Forms$@boolean%3AMapping%5BBoolean%5D): maps to `scala.Boolean`.
* [`checked`](api/scala/index.html#play.api.data.Forms$@checked%3AMapping%5BBoolean%5D): maps to `scala.Boolean`.

You also have the option of constructing your own custom validations using the [validation package](api/scala/index.html#play.api.data.validation.package).

### Validating a form in an Action

Now that we have constraints, we can validate the form inside an action, and process the form with errors.

We do this using the `fold` method, which takes two functions: the first is called if the binding fails, and the second is called if the binding succeeds.

@[userForm-handling-failure](code/ScalaForms.scala)

In the failure case, we render the page with BadRequest, and pass in the form _with errors_ as a parameter to the page.  If we use the view helpers (discussed below), then any errors that are bound to a field will be rendered in the page next to the field.

In the success case, we're sending a `Redirect` with a route to `routes.Application.home` here instead of rendering a view template.  This pattern is called  [Redirect after POST](http://en.wikipedia.org/wiki/Post/Redirect/Get), and is an excellent way to prevent duplicate form submissions.

> "Redirect after POST" is *required* when using `flashing` or other methods with [[flash scope|ScalaSessionFlash]], as new cookies will only be available after the redirected HTTP request.

### Showing forms in a view template

Once you have a form, then you need to make it available to the [[template engine|ScalaTemplates]].  You do this by including the form as a parameter to the view template.  For `user.scala.html`, the header at the top of the page will look like this:

```scala
@(userForm:Form[UserData])
```

Because user.scala.html needs a form passed in, you should pass the empty `userForm` initially when rendering `user.scala.html`:

XXX Replace with code snippet.

```scala
def index = Action {
  views.html.user(userForm)
}
```

The first thing is to be able to create the [form tag](api/scala/index.html#views.html.helper.form$). It is a simple view helper that creates a [form tag](http://www.w3.org/TR/html5/forms.html#the-form-element) and sets the `action` and `method` tag parameters according to the reverse route you pass in:

@[form-user](code/scalaguide/forms/scalaforms/views/user.scala.html)

You can find several input helpers in the [`views.html.helper`](api/scala/index.html#views.html.helper.package) package. You feed them with a form field, and they display the corresponding HTML input, setting the value, constraints and displaying errors when a form binding fails.

> You can use `@import helper._` in the template to avoid prefixing helpers with `@helper.`

There are several input helpers, but the most helpful are:

* [`form`](api/scala/index.html#views.html.helper.form$): renders a [form](http://www.w3.org/TR/html-markup/form.html#form) element.
* [`inputText`](api/scala/index.html#views.html.helper.inputText$): renders a [text input](http://www.w3.org/TR/html-markup/input.text.html) element.
* [`inputPassword`](api/scala/index.html#views.html.helper.inputPassword$): renders a [password input](http://www.w3.org/TR/html-markup/input.password.html#input.password) element.
* [`inputRadioGroup`](api/scala/index.html#views.html.helper.inputRadioGroup$): renders a [radio input](http://www.w3.org/TR/html-markup/input.radio.html#input.radio) element.
* [`inputDate`](api/scala/index.html#views.html.helper.inputDate$): renders a [date input](http://www.w3.org/TR/html-markup/input.date.html) element.
* [`inputFile`](api/scala/index.html#views.html.helper.inputFile$): renders a [file input](http://www.w3.org/TR/html-markup/input.file.html) element.
* [`select`](api/scala/index.html#views.html.helper.select$): renders a [select element](http://www.w3.org/TR/html-markup/select.html#select) element.
* [`textarea`](api/scala/index.html#views.html.helper.textarea$): renders a [textarea element](http://www.w3.org/TR/html-markup/textarea.html#textarea) element.
* [`checkbox`](api/scala/index.html#views.html.helper.checkbox$): renders a [checkbox element](http://www.w3.org/TR/html-markup/input.checkbox.html#input.checkbox) element.
* [`input`](api/scala/index.html#views.html.helper.input): renders a generic input element (which requires explicit arguments).

As with the `form` helper, you can specify an extra set of parameters that will be added to the generated Html:

@[form-user-parameters](code/scalaguide/forms/scalaforms/views/user.scala.html)

> **Note:** All extra parameters will be added to the generated Html, unless they start with the **\_** character. Arguments starting with **\_** are reserved for  [[field constructor arguments|ScalaCustomFieldConstructor]].

For complex form elements, you can also create your own [[custom view helpers|ScalaCustomViewHelpers]] and [[custom field constructors|ScalaCustomFieldConstructor]].

### Displaying errors in a view template

The errors in a form take the form of `Map[String,FormError]` where [`FormError`](api/scala/index.html#play.api.data.FormError) has:

* `key`: should be the same as the field.
* `message`: a message or a message key.
* `args`: a list of arguments to the message.

The form errors are accessed on the bound form instance as follows:

* `errors`: returns all errors as `Seq[FormError]`.
* `globalErrors`: returns errors without a key as `Seq[FormError]`.
* `error("name")`: returns the first error bound to key as `Option[FormError]`.
* `errors("name")`: returns all errors bound to key as `Seq[FormError]`.

Errors attached to a field will render automatically using the form helpers, so `@helper.inputText` with errors can display as follows:

@[form-user-generated](code/scalaguide/forms/scalaforms/views/user.scala.html)

Global errors that are not bound to a key do not have a helper and must be defined explicitly in the page:

```
@if(userForm.hasGlobalErrors) {
  <ul>
  @userForm.globalErrors.foreach { error =>
    <li>error.message</li>
  }
  </ul>
}
```

### Using Tuples

You can use tuples instead of case classes in your fields.

XXX Provide an form example.

XXX Provide a view helper example.

Using a tuple can be more convenient than defining a case class, especially for smaller forms.

### Fill a form with initial default values

Sometimes you’ll want to populate a form with existing values, typically for editing data:

@[userForm-filled](code/ScalaForms.scala)

When you use this with a view helper, the value of the element will be filled with with the value you pased in.

XXX Include view helper example


### Nested values

A form mapping can define nested values:

@[userForm-nested](code/ScalaForms.scala)

When you are using nested data this way, the form values sent by the browser must be named like `address.street`, `address.city`, etc.

XXX Include view helper example

### Repeated values

A form mapping can also define repeated values:

@[userListData](code/ScalaForms.scala)

@[userForm-repeated](code/ScalaForms.scala)

When you are using repeated data like this, the form values sent by the browser must be named `emails[0]`, `emails[1]`, `emails[2]`, etc.

Now you have to generate as many inputs for the `emails` field as the form contains. Just use the `repeat` helper for that:

@[form-field-repeat](code/scalaguide/forms/scalaforms/views/repeat.scala.html)

The `min` parameter allows you to display a minimum number of fields even if the corresponding form data are empty.

### Optional values

A form mapping can also define optional values:

@[userForm-optional](code/ScalaForms.scala)

### Defining ad-hoc constraints

You can also define ad-hoc constraints on the fields:

@[userForm-constraints-ad-hoc](code/ScalaForms.scala)

### Ignored values

If you want a form to have a static value for a field:

@[userForm-static-value](code/ScalaForms.scala)

Now you can mix optional, nested and repeated mappings any way you want to create complex forms.

> **Next:** [[Using the form template helpers | ScalaFormHelpers]]
