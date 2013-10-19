# Cookbook

## Rules

### Dependant values

```scala
val passRule = From[JsValue] { __ =>
  ((__ \ "password").read(notEmpty) ~
   (__ \ "verify").read(notEmpty)).tupled
    .compose(Rule.uncurry(Rules.equalTo[String])
    .repath(_ => (Path \ "verify")))
}

val rule = From[JsValue] { __ =>
  ((__ \ "login").read(notEmpty) ~ passRule).tupled
}
```

### Recursive types

```scala
case class User(
	name: String,
	age: Int,
	email: Option[String],
	isAlive: Boolean,
	friend: Option[User])
```

```scala
import play.api.libs.json._
import play.api.data.mapping._

implicit lazy val userRule = From[JsValue] { __ =>
	(__ \ "name").read[String] and
	(__ \ "age").read[Int] and
	(__ \ "email").read[Option[String]] and
	(__ \ "isAlive").read[Boolean] and
	(__ \ "friend").read[Option[User]]
}
```

or using macros:

```scala
import play.api.libs.json._
import play.api.data.mapping._

implicit lazy val userRule = Rule.gen[JsValue, User]
```

### Read keys