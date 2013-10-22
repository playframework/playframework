# Cookbook

> All the examples below are validating Json objects. The API is not dedicated only to Json, it can be used on any type. Please refer to [[Validating Json | ScalaValidationJson]], [[Validating Forms|ScalaValidationJson]], and [[Supporting new types|ScalaValidationExtension]] for more informations.

## `Rule`

### Typical case class validation

```scala
import play.api.libs.json._
import play.api.data.mapping._

case class Creature(
  name: String,
  isDead: Boolean,
  weight: Float)

implicit val creatureRule = From[JsValue]{ __ =>
	import play.api.data.mapping.json.Rules._
	(
	  (__ \ "name").read[String] ~
	  (__ \ "isDead").read[Boolean] ~
	  (__ \ "weight").read[Float]
	)(Creature)
}

val js = Json.obj( "name" -> "gremlins", "isDead" -> false, "weight" -> 1.0F)
From[JsValue, Creature](js) // Success(Creature(gremlins,false,1.0))

From[JsValue, Creature](Json.obj())
// Failure(List(
//	(/name, List(ValidationError(validation.required,WrappedArray()))),
//	(/isDead, List(ValidationError(validation.required,WrappedArray()))),
//	(/weight, List(ValidationError(validation.required,WrappedArray())))))
```

### Dependant values

A common example of this use case is the validation of `password` and `password confirmation` field in a signup form.

1. First, you need to validate that each field is valid independently
2. Then, given the two values, you need to validate that they are equals.


```scala
import play.api.libs.json._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

import play.api.data.mapping._

val passRule = From[JsValue] { __ =>
	import play.api.data.mapping.json.Rules._

  ((__ \ "password").read(notEmpty) ~
   (__ \ "verify").read(notEmpty)).tupled
    .compose(Rule.uncurry(Rules.equalTo[String])
    .repath(_ => (Path \ "verify")))
}
```

Splitting up the code:

```scala
((__ \ "password").read(notEmpty) ~
   (__ \ "verify").read(notEmpty)).tupled
```

This code creates a `Rule[JsValue, (String, String)]` each of of the String must be non-empty

```scala
Rule.uncurry(Rules.equalTo[String])
```
We then create a `Rule[(String, String), String]` validating that given a `(String, String)`, both strings are equals. Those rules are then composed together.

In case of `Failure`, we want to control the field holding the errors. We change the `Path` of errors using `repath`:

```scala
.repath(_ => (Path \ "verify")))
```

Let's test it:

```scala
passRule.validate(Json.obj("password" -> "foo", "verify" -> "foo")) // Success(foo)
passRule.validate(Json.obj("password" -> "", "verify" -> "foo")) // Failure(List((/password,List(ValidationError(validation.nonemptytext,WrappedArray())))))
passRule.validate(Json.obj("password" -> "foo", "verify" -> "")) // Failure(List((/verify,List(ValidationError(validation.nonemptytext,WrappedArray())))))
passRule.validate(Json.obj("password" -> "", "verify" -> "")) // Failure(List((/password,List(ValidationError(validation.nonemptytext,WrappedArray()))), (/verify,List(ValidationError(validation.nonemptytext,WrappedArray())))))
passRule.validate(Json.obj("password" -> "foo", "verify" -> "bar")) // Failure(List((/verify,List(ValidationError(validation.equals,WrappedArray(foo))))))
```

### Recursive types

When validating recursive types:

- Use the `lazy` keyword to allow forward reference.
- As with any recursive definition, the type of the `Rule` **must** be explicitly given.

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

// Note the lazy keyword, and the explicit typing
implicit lazy val userRule: Rule[JsValue, User] = From[JsValue] { __ =>
	import play.api.data.mapping.json.Rules._

	((__ \ "name").read[String] and
	 (__ \ "age").read[Int] and
	 (__ \ "email").read[Option[String]] and
	 (__ \ "isAlive").read[Boolean] and
	 (__ \ "friend").read[Option[User]])(User.apply _)
}
```

or using macros:

```scala
import play.api.libs.json._
import play.api.data.mapping._
import play.api.data.mapping.json.Rules._

// Note the lazy keyword, and the explicit typing
implicit lazy val userRule: Rule[JsValue, User] = Rule.gen[JsValue, User]
```

### Read keys

```scala
import play.api.libs.json._
import play.api.data.mapping._

val js = Json.parse("""
{
	"values": [
		{ "foo": "bar" },
		{ "bar": "baz" }
	]
}
""")

val r = From[JsValue] { __ =>
	import play.api.data.mapping.json.Rules._
	val tupleR = Rule.fromMapping[JsValue, (String, String)]{
		case JsObject((key, JsString(value)) :: Nil) =>  Success(key -> value)
		case _ => Failure(Seq(ValidationError("BAAAM")))
	}

	(__ \ "values").read(seq(tupleR))
}

r.validate(js) // Success(List((foo,bar), (bar,baz)))
```

### Validate subclasses (and parse the concrete class)

Consider the following class definitions:

```scala
trait A { val name: String }
case class B(name: String, foo: Int) extends A
case class C(name: String, bar: Int) extends A

val b = Json.obj("name" -> "B", "foo" -> 4)
val c = Json.obj("name" -> "C", "bar" -> 6)
val e = Json.obj("name" -> "E", "eee" -> 6)
```

#### Trying all the possible rules implementations

```scala

val rb: Rule[JsValue, A] = From[JsValue]{ __ =>
  ((__ \ "name").read[String] ~ (__ \ "foo").read[Int])(B.apply _)
}

val rc: Rule[JsValue, A] = From[JsValue]{ __ =>
  ((__ \ "name").read[String] ~ (__ \ "bar").read[Int])(C.apply _)
}

val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
val rule = rb orElse rc orElse Rule(_ => typeFailure)

rule.validate(b) // Success(B("B", 4))
rule.validate(c) // Success(C("C", 6))
rule.validate(e) // Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
```

#### Using class discovery based on field discrimination

```scala
 val typeFailure = Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))

 val rule = From[JsValue] { __ =>
  (__ \ "name").read[String].flatMap[A] {
    case "B" => ((__ \ "name").read[String] ~ (__ \ "foo").read[Int])(B.apply _)
    case "C" => ((__ \ "name").read[String] ~ (__ \ "bar").read[Int])(C.apply _)
    case _ => Rule(_ => typeFailure)
  }
}

rule.validate(b) // Success(B("B", 4))
rule.validate(c) // Success(C("C", 6))
rule.validate(e) // Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))
```

## `Write`

### typical case class `Write`

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

val c = To[Creature, JsObject](Creature("gremlins", false, 1f)) // {"name":"gremlins","isDead":false,"weight":1.0}
```

### Adding static values to a `Write`

```scala
import play.api.libs.json._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

import play.api.data.mapping._

case class LatLong(lat: Float, long: Float)
object LatLong {
	import play.api.data.mapping.json.Writes._
	implicit val write = Write.gen[LatLong, JsObject]
}

case class Point(coords: LatLong)
object Point {
	import play.api.data.mapping.json.Writes._
  implicit val write =
    (Write.gen[Point, JsObject] ~
     (Path \ "type").write[String, JsObject])((_: Point) -> "point")
}

val p = Point(LatLong(123.3F, 334.5F))
Point.write.writes(p) // {"coords":{"lat":123.3,"long":334.5},"type":"point"}
```





