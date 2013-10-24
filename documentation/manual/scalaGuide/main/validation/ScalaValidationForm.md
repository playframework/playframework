# Form template helpers

The validation API is compatible with play's existing [[template helpers|ScalaForms]].
There's one difference though : **you have to use `play.api.data.mapping.Form`** instead of using `play.api.data.Form`.

This `Form` helpers use `Rule` to validate data, and `Write` to format form data:

## Fill a form with initial default values

Consider the following example, from play's sample "computer database" application :

The `edit` action renders a form pre-filled with `computer` data. All we have to do is to create a `Form` instance using `Form.fill`:

@[form-edit](code/ScalaValidationForm.scala)

> Note we are using `play.api.data.mapping.Form`, **NOT** `play.api.data.Form`

Note that `Form.fill` needs to find an implicit `Write[Computer, UrlFormEncoded]`. In this example, we define it in `Application.scala`:

@[form-computer-write](code/ScalaValidationForm.scala)

Not only the write object serializes primitive types but it formats data when needed. 
In our example, dates will be displayed in the "yyyy-MM-dd" format.

`Form.fill` writes a `Computer` using `computerW`, and the resulting `Map[String, Seq[String]]` is then used by the `editForm` templates:

```scala
@(id: Long, computerForm: play.api.data.mapping.Form[Computer], companies : Seq[(String, String)])

@main {
	// form definition
}
```

From there, all the [[template helpers|ScalaForms]] work exactly as they used to :

```scala
@inputText(computerForm("name"), '_label -> "Computer name")
```

## Binding form data

The Form object uses a `Rule` to bind and validate data from a request body :

@[form-update](code/ScalaValidationForm.scala)

The Rule `computerValidation` is defined below.
Any custom format should match the `Form.fill` definition and be specified as a `read()` option.

@[form-computer-rule](code/ScalaValidationForm.scala)

> **Next:** - [[Validation Inception|ScalaValidationMacros]]
