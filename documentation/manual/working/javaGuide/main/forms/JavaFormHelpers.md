<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Form template helpers

Play provides several helpers to help you render form fields in HTML templates.

## Creating a `<form>` tag
    
The first helper creates the `<form>` tag. It is a pretty simple helper that automatically sets the `action` and `method` tag parameters according to the reverse route you pass in:

@[form](code/javaguide/forms/helpers.scala.html)

You can also pass an extra set of parameters that will be added to the generated HTML:

@[form-with-id](code/javaguide/forms/helpers.scala.html)

## Rendering an `<input>` element

There are several input helpers in the `views.html.helper` package. You feed them with a form field, and they display the corresponding HTML form control, with a populated value, constraints and errors:

@[full-form](code/javaguide/forms/fullform.scala.html)

As for the `form` helper, you can specify an extra set of parameters that will be added to the generated HTML:

@[extra-params](code/javaguide/forms/helpers.scala.html)

> **Note:** All extra parameters will be added to the generated HTML, except for ones whose name starts with the `_` character. Arguments starting with an underscore are reserved for field constructor argument (which we will see later).

## Handling HTML input creation yourself

There is also a more generic `input` helper that let you code the desired HTML result:

@[generic-input](code/javaguide/forms/helpers.scala.html)

## Field constructors

A rendered field does not only consist of an `<input>` tag, but may also need a `<label>` and a bunch of other tags used by your CSS framework to decorate the field.
    
All input helpers take an implicit `FieldConstructor` that handles this part. The default one (used if there are no other field constructors available in the scope), generates HTML like:

```
<dl class="error" id="email_field">
    <dt><label for="email">Email:</label></dt>
    <dd><input type="text" name="email" id="email" value=""></dd>
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

### Writing your own field constructor

Often you will need to write your own field constructor. Start by writing a template like:

@[template](code/javaguide/forms/myFieldConstructorTemplate.scala.html)

Save it in `views/` and name `myFieldConstructorTemplate.scala.html`

> **Note:** This is just a sample. You can make it as complicated as you need. You have also access to the original field using `@elements.field`.

Now create a `FieldConstructor` somewhere, using:

@[field](code/javaguide/forms/withFieldConstructor.scala.html)

## Handling repeated values

The last helper makes it easier to generate inputs for repeated values. Suppose you have this kind of form definition:

@[code](code/javaguide/forms/html/UserForm.java)

Now you have to generate as many inputs for the `emails` field as the form contains. Just use the `repeat` helper for that:

@[repeat](code/javaguide/forms/helpers.scala.html)

Use the `min` parameter to display a minimum number of fields, even if the corresponding form data are empty.
