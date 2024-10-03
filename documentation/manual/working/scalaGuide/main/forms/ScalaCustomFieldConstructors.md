<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Custom Field Constructors

A field rendering is not only composed of the `<input>` tag, but it also needs a `<label>` and possibly other tags used by your CSS framework to decorate the field.

All input helpers take an implicit [`FieldConstructor`](api/scala/views/html/helper/FieldConstructor.html) that handles this part. The [default one](api/scala/views/html/helper/defaultFieldConstructor$.html) (used if there are no other field constructors available in the scope), generates HTML like:

```html
<dl class="error" id="username_field">
    <dt><label for="username">Username:</label></dt>
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

## Writing your own field constructor

Often you will need to write your own field constructor. Start by creating a file `app/views/helper/myFieldConstructorTemplate.scala.html`:

@[form-myfield](code/scalaguide/forms/scalafieldconstructor/myFieldConstructorTemplate.scala.html)

> **Note:** This is just a sample. You can make it as complicated as you need. You also have access to the original field using `@elements.field`.

Now create a [`FieldConstructor`](api/scala/views/html/helper/FieldConstructor.html) using this template function in `app/views/helper/MyHelpers.scala`:

@[form-myfield-helper](code/ScalaFieldConstructor.scala)

And to make the form helpers use it, just import it in your templates:

@[import-myhelper](code/scalaguide/forms/scalafieldconstructor/userImport.scala.html)

@[form](code/scalaguide/forms/scalafieldconstructor/userImport.scala.html)

It will then use your field constructor to render the input text.

You can also set an implicit value for your [`FieldConstructor`](api/scala/views/html/helper/FieldConstructor.html) inline:

@[declare-implicit](code/scalaguide/forms/scalafieldconstructor/userDeclare.scala.html)

@[form](code/scalaguide/forms/scalafieldconstructor/userDeclare.scala.html)

## Example Project Structure

For reference, here's a simple project structure for implementing custom field constructors:

```
app/
├── controllers/
│   └── HomeController.scala
├── views/
│   ├── helper/
│   │   ├── myFieldConstructorTemplate.scala.html
│   │   └── MyHelpers.scala
│   ├── index.scala.html
│   └── main.scala.html
└── conf/
    └── routes
```

This structure allows for easy organization and import of your custom field constructors across your Play application.
