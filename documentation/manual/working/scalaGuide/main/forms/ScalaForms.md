<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
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

Now that we have a case class, the next step is to define a [`Form`](api/scala/index.html#play.api.data.Form) structure.
The function of a Form is to transform form data into a bound instance of a case class, and we define it like follows:

@[userForm-define](code/ScalaForms.scala)

The [Forms](api/scala/index.html#play.api.data.Forms$) object defines the [`mapping`](api/scala/index.html#play.api.data.Forms$@mapping%5BR%2CA1%5D\(\(String%2CMapping%5BA1%5D\)\)\(\(A1\)%E2%87%92R\)\(\(R\)%E2%87%92Option%5BA1%5D\)%3AMapping%5BR%5D) method. This method takes the names and constraints of the form, and also takes two functions: an `apply` function and an `unapply` function.  Because UserData is a case class, we can plug its apply and unapply methods directly into the mapping method.

> **Note:** Maximum number of fields for a single tuple or mapping is 22 due to the way form handling is implemented. If you have more than 22 fields in your form, you should break down your forms using lists or nested values.

A form will create UserData instance with the bound values when given a Map:

@[userForm-generate-map](code/ScalaForms.scala)

But most of the time you'll use forms from within an Action, with data provided from the request. [`Form`](api/scala/index.html#play.api.data.Form) contains [`bindFromRequest`](api/scala/index.html#play.api.data.Form@bindFromRequest\(\)\(Request%5B_%5D\)%3AForm%5BT%5D), which will take a request as an implicit parameter.  If you define an implicit request, then `bindFromRequest` will find it.

@[userForm-generate-request](code/ScalaForms.scala)

> **Note:** There is a catch to using `get` here.  If the form cannot bind to the data, then `get` will throw an exception.  We'll show a safer way of dealing with input in the next few sections.

You are not limited to using case classes in your form mapping.  As long as the apply and unapply methods are properly mapped, you can pass in anything you like, such as tuples using the [`Forms.tuple`](api/scala/index.html#play.api.data.Forms$) mapping or model case classes.  However, there are several advantages to defining a case class specifically for a form:

* **Form specific case classes are convenient.**  Case classes are designed to be simple containers of data, and provide out of the box features that are a natural match with Form functionality.
* **Form specific case classes are powerful.**  Tuples are convenient to use, but do not allow for custom apply or unapply methods, and can only reference contained data by arity (_1, _2, etc.)
* **Form specific case classes are targeted specifically to the Form.**  Reusing model case classes can be convenient, but often models will contain additional domain logic and even persistence details that can lead to tight coupling.  In addition, if there is not a direct 1:1 mapping between the form and the model, then sensitive fields must be explicitly ignored to prevent a [parameter tampering](https://www.owasp.org/index.php/Web_Parameter_Tampering) attack.

### Defining constraints on the form

The `text` constraint considers empty strings to be valid.  This means that `name` could be empty here without an error, which is not what we want.  A way to ensure that `name` has the appropriate value is to use the `nonEmptyText` constraint.

@[userForm-constraints-2](code/ScalaForms.scala)

Using this form will result in a form with errors if the input to the form does not match the constraints:

```scala
val boundForm = userFormConstraints2.bind(Map("bob" -> "", "age" -> "25"))
boundForm.hasErrors must beTrue
```

The out of the box constraints are defined on the [Forms object](api/scala/index.html#play.api.data.Forms$):

* [`text`](api/scala/index.html#play.api.data.Forms$@text%3AMapping%5BString%5D): maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* [`nonEmptyText`](api/scala/index.html#play.api.data.Forms$@nonEmptyText%3AMapping%5BString%5D): maps to `scala.String`, optionally takes `minLength` and `maxLength`.
* [`number`](api/scala/index.html#play.api.data.Forms$@number%3AMapping%5BInt%5D): maps to `scala.Int`, optionally takes `min`, `max`, and `strict`.
* [`longNumber`](api/scala/index.html#play.api.data.Forms$@longNumber%3AMapping%5BLong%5D): maps to `scala.Long`, optionally takes `min`, `max`, and `strict`.
* [`bigDecimal`](api/scala/index.html#play.api.data.Forms$@bigDecimal%3AMapping%5BBigDecimal%5D): takes `precision` and `scale`.
* [`date`](api/scala/index.html#play.api.data.Forms$@date%3AMapping%5BDate%5D), [`sqlDate`](api/scala/index.html#play.api.data.Forms$@sqlDate%3AMapping%5BDate%5D), [`jodaDate`](api/scala/index.html#play.api.data.Forms$@jodaDate%3AMapping%5BDateTime%5D): maps to `java.util.Date`, `java.sql.Date` and `org.joda.time.DateTime`, optionally takes `pattern` and `timeZone`.
* [`jodaLocalDate`](api/scala/index.html#play.api.data.Forms$@jodaLocalDate%3AMapping%5BLocalDate%5D): maps to `org.joda.time.LocalDate`, optionally takes `pattern`.
* [`email`](api/scala/index.html#play.api.data.Forms$@email%3AMapping%5BString%5D): maps to `scala.String`, using an email regular expression.
* [`boolean`](api/scala/index.html#play.api.data.Forms$@boolean%3AMapping%5BBoolean%5D): maps to `scala.Boolean`.
* [`checked`](api/scala/index.html#play.api.data.Forms$@checked%3AMapping%5BBoolean%5D): maps to `scala.Boolean`.
* [`optional`](api/scala/index.html#play.api.data.Forms): maps to `scala.Option`.

### Defining ad-hoc constraints

You can define your own ad-hoc constraints on the case classes using the [validation package](api/scala/index.html#play.api.data.validation.package).

@[userForm-constraints](code/ScalaForms.scala)

You can also define ad-hoc constraints on the case classes themselves:

@[userForm-constraints-ad-hoc](code/ScalaForms.scala)

You also have the option of constructing your own custom validations.  Please see the [[custom validations|ScalaCustomValidations]] section for more details.

### Validating a form in an Action

Now that we have constraints, we can validate the form inside an action, and process the form with errors.

We do this using the `fold` method, which takes two functions: the first is called if the binding fails, and the second is called if the binding succeeds.

@[userForm-handling-failure](code/ScalaForms.scala)

In the failure case, we render the page with BadRequest, and pass in the form _with errors_ as a parameter to the page.  If we use the view helpers (discussed below), then any errors that are bound to a field will be rendered in the page next to the field.

In the success case, we're sending a `Redirect` with a route to `routes.Application.home` here instead of rendering a view template.  This pattern is called  [Redirect after POST](http://en.wikipedia.org/wiki/Post/Redirect/Get), and is an excellent way to prevent duplicate form submissions.

> **Note:** "Redirect after POST" is **required** when using `flashing` or other methods with [[flash scope|ScalaSessionFlash]], as new cookies will only be available after the redirected HTTP request.

Alternatively, you can use the `parse.form` [[body parser|ScalaBodyParsers]] that binds the content of the request to your form.

@[form-bodyparser](code/ScalaForms.scala)

In the failure case, the default behaviour is to return an empty BadRequest response. You can override this behaviour with your own logic. For instance, the following code is completely equivalent to the preceding one using `bindFromRequest` and `fold`.

@[form-bodyparser-errors](code/ScalaForms.scala)

### Showing forms in a view template

Once you have a form, then you need to make it available to the template engine.  You do this by including the form as a parameter to the view template.  For `user.scala.html`, the header at the top of the page will look like this:

@[form-define](code/scalaguide/forms/scalaforms/views/user.scala.html)

Because `user.scala.html` needs a form passed in, you should pass the empty `userForm` initially when rendering `user.scala.html`:

@[form-render](code/ScalaForms.scala)

The first thing is to be able to create the [form tag](api/scala/index.html#views.html.helper.form$). It is a simple view helper that creates a [form tag](http://www.w3.org/TR/html5/forms.html#the-form-element) and sets the `action` and `method` tag parameters according to the reverse route you pass in:

@[form-user](code/scalaguide/forms/scalaforms/views/user.scala.html)

You can find several input helpers in the [`views.html.helper`](api/scala/index.html#views.html.helper.package) package. You feed them with a form field, and they display the corresponding HTML input, setting the value, constraints and displaying errors when a form binding fails.

> **Note:** You can use `@import helper._` in the template to avoid prefixing helpers with `@helper.`

There are several input helpers, but the most helpful are:

* [`form`](api/scala/index.html#views.html.helper.form$): renders a [form](http://www.w3.org/TR/html-markup/form.html#form) element.
* [`inputText`](api/scala/index.html#views.html.helper.inputText$): renders a [text input](http://www.w3.org/TR/html-markup/input.text.html) element.
* [`inputPassword`](api/scala/index.html#views.html.helper.inputPassword$): renders a [password input](http://www.w3.org/TR/html-markup/input.password.html#input.password) element.
* [`inputDate`](api/scala/index.html#views.html.helper.inputDate$): renders a [date input](http://www.w3.org/TR/html-markup/input.date.html) element.
* [`inputFile`](api/scala/index.html#views.html.helper.inputFile$): renders a [file input](http://www.w3.org/TR/html-markup/input.file.html) element.
* [`inputRadioGroup`](api/scala/index.html#views.html.helper.inputRadioGroup$): renders a [radio input](http://www.w3.org/TR/html-markup/input.radio.html#input.radio) element.
* [`select`](api/scala/index.html#views.html.helper.select$): renders a [select](http://www.w3.org/TR/html-markup/select.html#select) element.
* [`textarea`](api/scala/index.html#views.html.helper.textarea$): renders a [textarea](http://www.w3.org/TR/html-markup/textarea.html#textarea) element.
* [`checkbox`](api/scala/index.html#views.html.helper.checkbox$): renders a [checkbox](http://www.w3.org/TR/html-markup/input.checkbox.html#input.checkbox) element.
* [`input`](api/scala/index.html#views.html.helper.input$): renders a generic input element (which requires explicit arguments).

As with the `form` helper, you can specify an extra set of parameters that will be added to the generated Html:

@[form-user-parameters](code/scalaguide/forms/scalaforms/views/user.scala.html)

The generic `input` helper mentioned above will let you code the desired HTML result:

@[form-user-parameters-html](code/scalaguide/forms/scalaforms/views/user.scala.html)

> **Note:** All extra parameters will be added to the generated Html, unless they start with the **\_** character. Arguments starting with **\_** are reserved for [[field constructor arguments|ScalaCustomFieldConstructors]].

For complex form elements, you can also create your own custom view helpers (using scala classes in the `views` package) and [[custom field constructors|ScalaCustomFieldConstructors]].

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

@[global-errors](code/scalaguide/forms/scalaforms/views/user.scala.html)

### Mapping with tuples

You can use tuples instead of case classes in your fields:

@[userForm-tuple](code/ScalaForms.scala)

Using a tuple can be more convenient than defining a case class, especially for low arity tuples:

@[userForm-tuple-example](code/ScalaForms.scala)

### Mapping with single

Tuples are only possible when there are multiple values.  If there is only one field in the form, use `Forms.single` to map to a single value without the overhead of a case class or tuple:

```scala
val singleForm = Form(
  single(
    "email" -> email
  )
)
```

```scala
val email = singleForm.bind(Map("email", "bob@example.com")).get
```

### Fill values

Sometimes you’ll want to populate a form with existing values, typically for editing data:

@[userForm-filled](code/ScalaForms.scala)

When you use this with a view helper, the value of the element will be filled with the value:

```html
@helper.inputText(filledForm("name")) @* will render value="Bob" *@
```

Fill is especially helpful for helpers that need lists or maps of values, such as the [`select`](api/scala/index.html#views.html.helper.select$) and [`inputRadioGroup`](api/scala/index.html#views.html.helper.inputRadioGroup$) helpers.  Use [`options`](api/scala/index.html#views.html.helper.options$) to value these helpers with lists, maps and pairs.

### Nested values

A form mapping can define nested values by using [`Forms.mapping`](api/scala/index.html#play.api.data.Forms$) inside an existing mapping:

@[userData-nested](code/ScalaForms.scala)

@[userForm-nested](code/ScalaForms.scala)

> **Note:** When you are using nested data this way, the form values sent by the browser must be named like `address.street`, `address.city`, etc.

@[form-field-nested](code/scalaguide/forms/scalaforms/views/nested.scala.html)

### Repeated values

A form mapping can define repeated values using [`Forms.list`](api/scala/index.html#play.api.data.Forms$) or [`Forms.seq`](api/scala/index.html#play.api.data.Forms$):

@[userListData](code/ScalaForms.scala)

@[userForm-repeated](code/ScalaForms.scala)

When you are using repeated data like this, there are two alternatives for sending the form values in the HTTP request.  First, you can suffix the parameter with an empty bracket pair, as in "emails[]".  This parameter can then be repeated in the standard way, as in `http://foo.com/request?emails[]=a@b.com&emails[]=c@d.com`.  Alternatively, the client can explicitly name the parameters uniquely with array subscripts, as in `emails[0]`, `emails[1]`, `emails[2]`, and so on.  This approach also allows you to maintain the order of a sequence of inputs.  

If you are using Play to generate your form HTML, you can generate as many inputs for the `emails` field as the form contains, using the [`repeat`](api/scala/index.html#views.html.helper.repeat$) helper:

@[form-field-repeat](code/scalaguide/forms/scalaforms/views/repeat.scala.html)

The `min` parameter allows you to display a minimum number of fields even if the corresponding form data are empty.

### Optional values

A form mapping can also define optional values using [`Forms.optional`](api/scala/index.html#play.api.data.Forms$):

@[userData-optional](code/ScalaForms.scala)

@[userForm-optional](code/ScalaForms.scala)

This maps to an `Option[A]` in output, which is `None` if no form value is found.

### Default values

You can populate a form with initial values using [`Form#fill`](api/scala/index.html#play.api.data.Form):

```
val filledForm = userForm.fill(User("Bob", 18))
```

Or you can define a default mapping on the number using [`Forms.default`](api/scala/index.html#play.api.data.Forms$):

```
Form(
  mapping(
    "name" -> default(text, "Bob")
    "age" -> default(number, 18)
  )(User.apply)(User.unapply)
)
```

### Ignored values

If you want a form to have a static value for a field, use [`Forms.ignored`](api/scala/index.html#play.api.data.Forms$):

@[userForm-static-value](code/ScalaForms.scala)

## Putting it all together

Here's an example of what a model and controller would look like for managing an entity.

Given the case class `Contact`:

@[contact-define](code/ScalaForms.scala)

Note that `Contact` contains a `Seq` with `ContactInformation` elements and a `List` of `String`.  In this case, we can combine the nested mapping with repeated mappings (defined with `Forms.seq` and `Forms.list`, respectively).

@[contact-form](code/ScalaForms.scala)

And this code shows how an existing contact is displayed in the form using filled data:

@[contact-edit](code/ScalaForms.scala)

Finally, this is what a form submission handler would look like:

@[contact-save](code/ScalaForms.scala)
