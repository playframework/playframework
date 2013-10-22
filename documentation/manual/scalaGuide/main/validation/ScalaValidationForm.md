# Form template helpers

The validation API is compatible with play's existing [[template helpers|ScalaFormHelpers]].
There's one difference thought, instead of using `play.api.data.Form`, **you need to use `play.api.data.mapping.Form`**.

This `Form` helpers using `Rule` to validate data, and `Write` to format form data:

## Fill a form with initial default values

Consider the following example form play sample application "computer database":

The `edit` action renders a form with all the computer data pre-filled. All we have to do is to create a `Form` instance using `Form.fill`:

```scala
def edit(id: Long) = Action {
  Computer.findById(id).map { computer =>
    Ok(html.editForm(id, Form.fill(computer), Company.options))
  }.getOrElse(NotFound)
}
```

> Note we are using `play.api.data.mapping.Form`, **NOT** `play.api.data.Form`

Note that `Form.fill` needs to find an implicit `Write[Computer, UrlFormEncoded]`. In this sample, that `Write` is defined in `Application.scala`:

```scala
implicit val computerW = To[UrlFormEncoded] { __ =>
  import play.api.data.mapping.Writes._
  ((__ \ "id").write[Pk[Long]] ~
   (__ \ "name").write[String] ~
   (__ \ "introduced").write(option(date("yyyy-MM-dd"))) ~
   (__ \ "discontinued").write(option(date("yyyy-MM-dd"))) ~
   (__ \ "company").write[Option[Long]]) (unlift(Computer.unapply _))
}
```

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

```scala
def update(id: Long) = Action(parse.urlFormEncoded) { implicit request =>
  val r = computerValidation.validate(request.body)
  r match {
    case Failure(_) => BadRequest(html.editForm(id, Form(request.body, r), Company.options))
    case Success(computer) => {
      Computer.update(id, computer)
      Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
    }
  }
}
```

The Rule `computerValidation` is defined bellow:

```scala
implicit val computerValidation = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.Rules._
    ((__ \ "id").read(ignored[UrlFormEncoded, Pk[Long]](NotAssigned)) ~
     (__ \ "name").read(notEmpty) ~
     (__ \ "introduced").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "discontinued").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "company").read[Option[Long]]) (Computer.apply _)
  }
```