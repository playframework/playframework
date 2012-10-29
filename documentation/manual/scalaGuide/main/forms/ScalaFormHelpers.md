# Using the form template helpers

Play provides several helpers for rendering form fields in HTML templates.

## Create a `<form>` tag
    
The first thing is to be able to create the `<form>` tag. It is a pretty simple helper that has no more value than automatically setting the `action` and `method` tag parameters according to the reverse route you pass in:
    
```
@helper.form(action = routes.Application.submit) {
    
}
```

You can also pass an extra set of parameters that will be added to the generated Html:

```
@helper.form(action = routes.Application.submit, 'id -> "myForm") {
    
}
```

## Rendering an `<input>` element

You can find several input helpers in the `views.html.helper` package. You feed them with a form field, and they display the corresponding HTML input, setting the value, constraints and errors:

```
@(myForm: Form[User])

@helper.form(action = routes.Application.submit) {
    
    @helper.inputText(myForm("username"))
    
    @helper.inputPassword(myForm("password"))
    
}
```

As for the `form` helper, you can specify an extra set of parameters that will be added to the generated Html:

```
@helper.inputText(myForm("username"), 'id -> "username", 'size -> 30)
```

> **Note:** All extra parameters will be added to the generated Html, unless they start with the **\_** character. Arguments starting with **\_** are reserved for field constructor arguments (we will see that shortly).

## Handling HTML input creation yourself

There is also a more generic `input` helper that lets you code the desired HTML result:

```
@helper.input(myForm("username")) { (id, name, value, args) =>
    <input type="date" name="@name" id="@id" @toHtmlArgs(args)>
} 
```

## Field constructors

A field rendering is not only composed of the `<input>` tag, but it also needs a `<label>` and possibly other tags used by your CSS framework to decorate the field.
    
All input helpers take an implicit `FieldConstructor` that handles this part. The default one (used if there are no other field constructors available in the scope), generates HTML like:

```
<dl class="error" id="username_field">
    <dt><label for="username"><label>Username:</label></dt>
    <dd><input type="text" name="username" id="username" value=""></dd>
    <dd class="error">This field is required!</dd>
    <dd class="error">Another error</dd>
    <dd class="info">Required</dd>
    <dd class="info">Another constraint</dd>
</dl>
```

This default field constructor supports additional options you can pass in the input helper arguments:

```
'_label -> "Custom label"
'_id -> "idForTheTopDlElement"
'_help -> "Custom help"
'_showConstraints -> false
'_error -> "Force an error"
'_showErrors -> false
```

## Twitter bootstrap field constructor

There is also another built-in field constructor that can be used with [[TwitterBootstrap | http://twitter.github.com/bootstrap/]] 1.4.  (Note: 2.0.2 will support LESS 1.3 and Bootstrap 2.0.)

To use it, just import it in the current scope:

```
@import helper.twitterBootstrap._
```

It generates Html like:

```
<div class="clearfix error" id="username_field">
    <label for="username">Username:</label>
    <div class="input">
        <input type="text" name="username" id="username" value="">
        <span class="help-inline">This field is required!, Another error</span>
        <span class="help-block">Required, Another constraint</span> 
    </div>
</div>
```

It supports the same set of options as the default field constructor (see below).

## Writing your own field constructor

Often you will need to write your own field constructor. Start by writing a template like:

```
@(elements: helper.FieldElements)

<div class="@if(elements.hasErrors) {error}">
    <label for="@elements.id">@elements.label</label>
    <div class="input">
        @elements.input
        <span class="errors">@elements.errors.mkString(", ")</span>
        <span class="help">@elements.infos.mkString(", ")</span> 
    </div>
</div>
```

> **Note:** This is just a sample. You can make it as complicated as you need. You also have access to the original field using `@elements.field`.

Now create a `FieldConstructor` using this template function:

```
object MyHelpers {
    
  implicit val myFields = FieldConstructor(myFieldConstructorTemplate.f)    
    
}
```

And to make the form helpers use it, just import it in your templates:

```
@import MyHelpers._

@inputText(myForm("username"))
```

It will then use your field constructor to render the input text.

> **Note:** You can also set an implicit value for your `FieldConstructor` inline in your template this way:
>
> ```
> @implicitField = @{ FieldConstructor(myFieldConstructorTemplate.f) }
>
> @inputText(myForm("username"))
> ```

## Handling repeated values

The last helper makes it easier to generate inputs for repeated values. Let’s say you have this kind of form definition:

```
val myForm = Form(
  tuple(
    "name" -> text,
    "emails" -> list(email)
  )
)
```

Now you have to generate as many inputs for the `emails` field as the form contains. Just use the `repeat` helper for that:

```
@inputText(myForm("name"))

@repeat(myForm("emails"), min = 1) { emailField =>
    
    @inputText(emailField)
    
}
```

The `min` parameter allows you to display a minimum number of fields even if the corresponding form data are empty.

> **Next:** [[Working with JSON| ScalaJson]]



