# Migration from the Form API

The new Validation API is rather different than the `Form` API.

```scala
case class Company(id: Pk[Long] = NotAssigned, name: String)
case class Computer(id: Pk[Long] = NotAssigned, name: String, introduced: Option[Date], discontinued: Option[Date], companyId: Option[Long])
```

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

```scala
implicit def pkW[I, O](implicit w: Path => Write[Option[I], O]) =
  (p: Path) => w(p).contramap((_: Pk[I]).toOption)

implicit val computerW = {
  import play.api.data.mapping.Writes._
  Write.gen[Computer, UrlFormEncoded]
}
```

### Filling a Form with an object

```scala
def edit(id: Long) = Action {
  Computer.findById(id).map { computer =>
    Ok(html.editForm(id, Form.fill(computer), Company.options))
  }.getOrElse(NotFound)
}
```

### Validating the submitted form

```scala
def save = Action(parse.urlFormEncoded) { implicit request =>
  val r = computerValidation.validate(request.body)
  r.fold(
    err => BadRequest(html.createForm(Form(request.body, r), Company.options)),
    computer => {
      Computer.insert(computer)
      Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
    }
  )
}
```