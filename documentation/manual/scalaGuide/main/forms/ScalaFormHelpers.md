# Using the form template helpers

Play provides several helpers for rendering form fields in HTML templates.

## Create a `<form>` tag
    
The first thing is to be able to create the `<form>` tag. It is a pretty simple helper that has no more value than automatically setting the `action` and `method` tag parameters according to the reverse route you pass in:

@[form-tag](code/scalaguide/forms/scalaformhelper/views/login.scala.html)


You can also pass an extra set of parameters that will be added to the generated Html:

@[form-pass-parameters](code/scalaguide/forms/scalaformhelper/views/login.scala.html)


## Rendering an `<input>` element

You can find several input helpers in the `views.html.helper` package. You feed them with a form field, and they display the corresponding HTML input, setting the value, constraints and errors:

@[form-define](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

@[form-user](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


As for the `form` helper, you can specify an extra set of parameters that will be added to the generated Html:

@[form-user-parameters](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


> **Note:** All extra parameters will be added to the generated Html, unless they start with the **\_** character. Arguments starting with **\_** are reserved for field constructor arguments (we will see that shortly).

## Handling HTML input creation yourself

There is also a more generic `input` helper that lets you code the desired HTML result:

@[form-user-parameters-html](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

## Field constructors

A field rendering is not only composed of the `<input>` tag, but it also needs a `<label>` and possibly other tags used by your CSS framework to decorate the field.
    
All input helpers take an implicit `FieldConstructor` that handles this part. The default one (used if there are no other field constructors available in the scope), generates HTML like:

@[form-user-generated](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


This default field constructor supports additional options you can pass in the input helper arguments:

@[form-field-options](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

## Twitter bootstrap field constructor

There is also another built-in field constructor that can be used with [TwitterBootstrap](http://twitter.github.com/bootstrap/) 1.4.  (Note: 2.0.2 will support LESS 1.3 and Bootstrap 2.0.)

To use it, just import it in the current scope:

@[import-twitterbootstrap](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


It generates Html like:

@[form-bootstrap-generated](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


It supports the same set of options as the default field constructor (see below).

## Writing your own field constructor

Often you will need to write your own field constructor. Start by writing a template like:

@[form-myfield](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


> **Note:** This is just a sample. You can make it as complicated as you need. You also have access to the original field using `@elements.field`.

Now create a `FieldConstructor` using this template function:

@[form-myfield-helper](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


And to make the form helpers use it, just import it in your templates:


@[import-myhelper](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

@[form-myfield-helper](code/scalaguide/forms/scalaformhelper/views/user.scala.html)


It will then use your field constructor to render the input text.

> **Note:** You can also set an implicit value for your `FieldConstructor` inline in your template this way:
>
>@[import-myhelper-implicit](code/scalaguide/forms/scalaformhelper/views/user.scala.html)
>
>@[form-myfield-helper](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

## Handling repeated values

The last helper makes it easier to generate inputs for repeated values. Let’s say you have this kind of form definition:

@[userForm-repeated](code/ScalaForms.scala)


Now you have to generate as many inputs for the `emails` field as the form contains. Just use the `repeat` helper for that:

@[form-field-repeat](code/scalaguide/forms/scalaformhelper/views/register.scala.html)

The `min` parameter allows you to display a minimum number of fields even if the corresponding form data are empty.

> **Next:** [[Protecting against CSRF|ScalaCsrf]]



