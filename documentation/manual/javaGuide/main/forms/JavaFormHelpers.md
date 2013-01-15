# Form template helpers

Play provides several helpers to help you render form fields in HTML templates.

## Creating a `<form>` tag
    
The first helper creates the `<form>` tag. It is a pretty simple helper that automatically sets the `action` and `method` tag parameters according to the reverse route you pass in:
    
```
@helper.form(action = routes.Application.submit()) {
    
}
```

You can also pass an extra set of parameters that will be added to the generated HTML:

```
@helper.form(action = routes.Application.submit(), 'id -> "myForm") {
    
}
```

## Rendering an `<input>` element

There are several input helpers in the `views.html.helper` package. You feed them with a form field, and they display the corresponding HTML form control, with a populated value, constraints and errors:

```
@(myForm: Form[User])

@helper.form(action = routes.Application.submit()) {
    
    @helper.inputText(myForm("username"))
    
    @helper.inputPassword(myForm("password"))
    
}
```

As for the `form` helper, you can specify an extra set of parameters that will be added to the generated HTML:

```
@helper.inputText(myForm("username"), 'id -> "username", 'size -> 30)
```

> **Note:** All extra parameters will be added to the generated HTML, except for ones whose name starts with the `_` character. Arguments starting with an underscore are reserved for field constructor argument (which we will see later).

## Handling HTML input creation yourself

There is also a more generic `input` helper that let you code the desired HTML result:

```
@helper.input(myForm("username")) { (id, name, value, args) =>
    <input type="date" name="@name" id="@id" @toHtmlArgs(args)>
} 
```

## Field constructors

A rendered field does not only consist of an `<input>` tag, but may also need a `<label>` and a bunch of other tags used by your CSS framework to decorate the field.
    
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

There is another built-in field constructor that can be used with [[Twitter Bootstrap | http://twitter.github.com/bootstrap/]].

To use it, just import it in the current scope:

```
@import helper.twitterBootstrap._
```

This field constructor generates HTML like the following:

```
<div class="clearfix error" id="username_field">
  <label for="username">Username:</label>
  <div class="input">
    <input type="text" name="username" id="username" value="">
    <span class="help-inline">This field is required!, Another error</span>
    <span class="help-block">Required, Another constraint</d</span> 
  </div>
</div>
```

It supports the same set of options as the default field constructor (see above).

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

> **Note:** This is just a sample. You can make it as complicated as you need. You have also access to the original field using `@elements.field`.

Now create a `FieldConstructor` somewhere, using:

```
@implicitField = @{ FieldConstructor(myFieldConstructorTemplate.f) }

@inputText(myForm("username"))
```

## Handling repeated values

The last helper makes it easier to generate inputs for repeated values. Suppose you have this kind of form definition:

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

Use the `min` parameter to display a minimum number of fields, even if the corresponding form data are empty.

> **Next:** [[Working with JSON| JavaJsonRequests]]



