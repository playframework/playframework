
## Field constructors

A field rendering is not only composed of the `<input>` tag, but it also needs a `<label>` and possibly other tags used by your CSS framework to decorate the field.

All input helpers take an implicit `FieldConstructor` that handles this part. The default one (used if there are no other field constructors available in the scope), generates HTML like:

@[form-user-generated](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

This default field constructor supports additional options you can pass in the input helper arguments:

@[form-field-options](code/scalaguide/forms/scalaformhelper/views/user.scala.html)

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
