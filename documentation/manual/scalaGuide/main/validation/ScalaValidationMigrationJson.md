# Migration from the Json API

The Json API and the new validation API are really similar. One could see the new Validation API as just an evolution of the Json API.

> The json validation API **still works just fine** but we recommend you use the new validation API for new code, and to port your old code whenever it's possible.

## `Reads` migration

The equivalent of a Json `Reads` is a `Rule`. The key difference is that `Reads` assumes Json input, while `Rule` is more generic, and therefore has one more type parameter.

Basically `Reads[String]` == `Rule[JsValue, String]`.

Migrating a Json `Reads` to a `Rule` is just a matter of modifying imports and specifying the input type.

Let's take a typical example from the Json API documentation:

@[migration-creature](code/ScalaValidationMigrationJson.scala)

Using the json API, you would have defined something like:

@[migration-creature-old](code/ScalaValidationMigrationJson.scala)

Using the new API, this code becomes:

@[migration-creature-new](code/ScalaValidationMigrationJson.scala)

Which appart from the extra imports is very similar. Notice the `From[JsValue]{...}` block, that's one of the nice features of the new validation API. Not only it avoids type repetition, but it also scopes the implicits.

> **Important:** Note that we're importing `Rules._` **inside** the `From[JsValue]{...}` block.
It is recommended to always follow this pattern, as it nicely scopes the implicits, avoiding conflicts and accidental shadowing.

### readNullable

The readNullable method does not exists anymore. Just use a `Rule[JsValue, Option[T]]` instead. `null` and non existing Path will be handled correctly and give you a `None`:

@[migration-readNullable](code/ScalaValidationMigrationJson.scala)

### keepAnd

The general use for `keepAnd` is to apply two validation on the same `JsValue`, for example:

@[migration-keepAnd-old](code/ScalaValidationMigrationJson.scala)

You can achieve the same think in the Validation API using [[Rules composition|ScalaValidationRule]]

@[migration-keepAnd-new](code/ScalaValidationMigrationJson.scala)

### lazy reads

Reads are always lazy in the new validation API, therefore you don't need to use any specific function, even for recursive types:

@[migration-lazy-old](code/ScalaValidationMigrationJson.scala)

becomes:

@[migration-lazy-new](code/ScalaValidationMigrationJson.scala)

### Numeric types

You should be aware that numeric type coercion is a bit stricter in the validation API.

For example:

```scala
val js = Json.obj("n" -> 42.5f)
js.validate((__ \ "n").read[Int]) // JsSuccess(42, /n)
```

whereas with the validation API, an `Int` must really be an `Int`:

```scala
val js = Json.obj("n" -> 42.5f)
(Path \ "n").read[JsValue, Int].validate(js) // Failure(List((/n,List(ValidationError(validation.type-mismatch,WrappedArray(Int))))))
```

### `json.apply` and `path.as[T]`

Those methods do not exist in the validation API. Even in the json API, it is generally recommended not to use them as they are "unsafe".

The preferred solution is to use `path.read[T]` and to handle failure properly.

```scala
(__ \ "foo").as[String]
```

becomes

```scala
(Path \ "foo").read[JsValue, String]
```

### pickBranch

`JsPath` has a `prickBranch` method, that creates a `Reads` extracting a subtree in a Json object:

For example, given the following json object, we can extract a sub tree:

```scala
import play.api.libs.json._

val js = Json.obj(
	"field1" -> "alpha",
	"field2" -> 123L,
	"field3" -> Json.obj(
	  "field31" -> "beta",
	  "field32"-> 345
	))

val pick = (__ \ "field3").json.pickBranch
pick.reads(js) // Success({"field3":{"field31":"beta","field32":345}})
```

In the validation API, you simply use `read` to create a rule picking a branch:

```scala
import play.api.libs.json._
import play.api.data.mapping._

val js = Json.obj(
	"field1" -> "alpha",
	"field2" -> 123L,
	"field3" -> Json.obj(
	  "field31" -> "beta",
	  "field32"-> 345
	))

val pick = From[JsValue]{ __ =>
	import play.api.data.mapping.json.Rules._
	(__ \ "field3").read[JsValue]
}

pick.validate(js) // Success({"field31":"beta","field32":345})
```

## `Writes` migration

`Writes` are really easy to port. Just like `Reads`, it's basically a matter of adding imports.

For example, you would have defined a `Writes` for the `Creature` case class this way:

```scala
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Creature(
  name: String,
  isDead: Boolean,
  weight: Float)

implicit val creatureWrite =
	(
	  (__ \ "name").write[String] ~
	  (__ \ "isDead").write[Boolean] ~
	  (__ \ "weight").write[Float]
	)(unlift(Creature.unapply _))

val c = Json.toJson(Creature("gremlins", false, 1f)) // c: JsValue = {"name":"gremlins","isDead":false,"weight":1.0}
```

With the validation API:

```scala
import play.api.libs.json._
import play.api.data.mapping._
import play.api.libs.functional.syntax.unlift

case class Creature(
  name: String,
  isDead: Boolean,
  weight: Float)

implicit val creatureWrite = To[JsObject]{ __ =>
	import play.api.data.mapping.json.Writes._
	(
	  (__ \ "name").write[String] ~
	  (__ \ "isDead").write[Boolean] ~
	  (__ \ "weight").write[Float]
	)(unlift(Creature.unapply _))
}

val c = To[Creature, JsObject](Creature("gremlins", false, 1f)) // c: JsObject = {"name":"gremlins","isDead":false,"weight":1.0}
```

## `Format` migration

The validation API does not have an equivalent for `Format`. We find that generally `Format` is not really convenient since validation and serialization are rarely symmetrical, and you quite often end up havind multiple `Reads` for a given type, making `Format` rather unsettling.

## Json Inception (macro)

Macros are also available for the validation API. See [[Validation Inception|ScalaValidationMacros]].

> **Next:** - [[Migration from 2.1.x Form API|ScalaValidationMigrationForm]]