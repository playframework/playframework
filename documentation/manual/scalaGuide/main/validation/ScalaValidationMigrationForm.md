# Form API migration

Although the new Validation API differs significantly from the `Form` API, migrating to to new API is straightforward.
This example is a case study of the migration of one of play sample application: "computer database".

We'll consider `Application.scala`. This controller take care of Computer creation, and edition. The models are defined in `Models.scala`

```scala
case class Company(id: Pk[Long] = NotAssigned, name: String)
case class Computer(id: Pk[Long] = NotAssigned, name: String, introduced: Option[Date], discontinued: Option[Date], companyId: Option[Long])
```

Here's the `Application` controller, **before migration**:

```scala
package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import anorm._

import views._
import models._

object Application extends Controller {


  /**
   * Describe the computer form (used in both edit and create screens).
   */
  val computerForm = Form(
    mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "name" -> nonEmptyText,
      "introduced" -> optional(date("yyyy-MM-dd")),
      "discontinued" -> optional(date("yyyy-MM-dd")),
      "company" -> optional(longNumber)
    )(Computer.apply)(Computer.unapply)
  )

  def index = // ...

  def list(page: Int, orderBy: Int, filter: String) = // ...

  def edit(id: Long) = Action {
    Computer.findById(id).map { computer =>
      Ok(html.editForm(id, computerForm.fill(computer), Company.options))
    }.getOrElse(NotFound)
  }

  def update(id: Long) = Action { implicit request =>
    computerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.editForm(id, formWithErrors, Company.options)),
      computer => {
        Computer.update(id, computer)
        Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
      }
    )
  }

  def create = Action {
    Ok(html.createForm(computerForm, Company.options))
  }

  def save = Action { implicit request =>
    computerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.createForm(formWithErrors, Company.options)),
      computer => {
        Computer.insert(computer)
        Home.flashing("success" -> "Computer %s has been created".format(computer.name))
      }
    )
  }

  def delete(id: Long) = // ...

}

```

### Validation rules migration

The first thing we must change is the definition of the `Computer` validations.
Instead of using `play.api.data.Form`, we must define a `Rule[UrlFormEncoded, Computer]`.

`UrlFormEncoded` is simply an alias for `Map[String, Seq[String]]`, which is the type used by play for form encoded request bodies.

Even though the syntax looks different, the logic is basically the same.

```scala
import play.api.data.mapping._

// ...

implicit val computerValidation = From[UrlFormEncoded] { __ =>
  import play.api.data.mapping.Rules._
  ((__ \ "id").read(ignored[UrlFormEncoded, Pk[Long]](NotAssigned)) ~
   (__ \ "name").read(notEmpty) ~
   (__ \ "introduced").read(option(date("yyyy-MM-dd"))) ~
   (__ \ "discontinued").read(option(date("yyyy-MM-dd"))) ~
   (__ \ "company").read[Option[Long]]) (Computer.apply _)
}
```

You start by defining a simple validation for each field.

For example `"name" -> nonEmptyText` now becomes `(__ \ "name").read(notEmpty)`
The next step is to compose these validations together, to get a new validation.

The *old* api does that using a function called `mapping`, the validation api uses a method called `~` or `and` (`and` is a alias).

```scala
mapping(
  "name" -> nonEmptyText,
  "introduced" -> optional(date("yyyy-MM-dd"))
```

now becomes

```scala
(__ \ "name").read(notEmpty) ~
(__ \ "introduced").read(option(date("yyyy-MM-dd")))
```

A few built-in validations have a slightly different name than in the Form api, like `optional` that became `option`. You can find all the built-in rules in the scaladoc.

> **Be careful with your imports**. Some rules have the same names than form mapping, which could make the implicit parameters resolution fail silently.


### Filling a `Form` with an object

The new validation API comes with a `Form` class. This class is fully compatible with the existing form input helpers.
You can use the `Form.fill` method to create a `Form` from a class.

`Form.fill` needs an instance of `Write[T, UrlFormEncoded]`, where `T` is your class type.

```scala
implicit def pkW[I, O](implicit w: Path => Write[Option[I], O]) =
  (p: Path) => w(p).contramap((_: Pk[I]).toOption)

implicit val computerW = To[UrlFormEncoded] { __ =>
  import play.api.data.mapping.Writes._
  ((__ \ "id").write[Pk[Long]] ~
   (__ \ "name").write[String] ~
   (__ \ "introduced").write(option(date("yyyy-MM-dd"))) ~
   (__ \ "discontinued").write(option(date("yyyy-MM-dd"))) ~
   (__ \ "company").write[Option[Long]]) (unlift(Computer.unapply _))
}
```

> Note that this `Write` takes care of formatting.

We can then create a `Form` for a computer instance using `Form.fill`, and pass it to the view:

```scala
def edit(id: Long) = Action {
  Computer.findById(id).map { computer =>
    Ok(html.editForm(id, Form.fill(computer), Company.options))
  }.getOrElse(NotFound)
}
```

> The type of `Form` in the view parameters is now: `@(..., computerForm: play.api.data.mapping.Form[Computer], ...)`

### Validating the submitted form

Handling validation errors is vastly similar, the main difference is that `bindFromRequest` does not exist anymore.

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

> **Next:** - [[Cookbook | ScalaValidationCookbook]]
