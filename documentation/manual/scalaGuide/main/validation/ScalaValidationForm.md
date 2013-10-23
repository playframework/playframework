# Form template helpers

The validation API is compatible with play's existing [[template helpers|ScalaFormHelpers]].
There's one difference thought, instead of using `play.api.data.Form`, **you need to use `play.api.data.mapping.Form`**.

This `Form` helpers using `Rule` to validate data, and `Write` to format form data:

## Fill a form with initial default values

Consider the following example form play sample application "computer database":

The `edit` action renders a form with all the computer data pre-filled. All we have to do is to create a `Form` instance using `Form.fill`:

@[form-edit](code/ScalaValidationForm.scala)

> Note we are using `play.api.data.mapping.Form`, **NOT** `play.api.data.Form`

Note that `Form.fill` needs to find an implicit `Write[Computer, UrlFormEncoded]`. In this sample, that `Write` is defined in `Application.scala`:

@[form-computer-write](code/ScalaValidationForm.scala)

The write object is not only used to serialize primitive types, it also formats data.
For example, we want dates to be display using this format: "yyyy-MM-dd".

`Form.fill` writes a `Computer` using `computerW`, and the resulting `Map[String, Seq[String]]` is then used by the `editForm` templates:

```scala
@(id: Long, computerForm: play.api.data.mapping.Form[Computer], companies : Seq[(String, String)])

@main {
	// form definition
}
```

From there, all the [[template helpers|ScalaFormHelpers]] work exactly as they used to. Form example to create an input of type text:

```scala
@inputText(computerForm("name"), '_label -> "Computer name")
```

## Binding form data

The Form object simply uses a `Rule` to bind and validate data from a request body.
Here's a example:

@[form-update](code/ScalaValidationForm.scala)

The Rule `computerValidation` is defined bellow:

@[form-computer-rule](code/ScalaValidationForm.scala)

> **Next:** - [[Validation Inception|ScalaValidationMacros]]