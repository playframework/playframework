# Extensions: Supporting new types

The validation API is designed to be easily extensible. Supporting new types is just a matter of providing the appropriate set of Rules and Writes.

In this documentation, we'll study the implementation of the Json support. All extensions are to be defined in a similar fashion. The total amount of code needed is rather small, but there's best practices you need to follow.

## Rules

The first step is to define what we call primitive rules. Primitive rules is a set of rules on which you could build any complex validation.

The base of all Rules is the capacity to extract a subset of some input data.

For the type `JsValue`, we need to be able to extract a `JsValue` at a given `Path`:

```scala
object Rules {
	import play.api.data.mapping._

	implicit def pickInJson[O](p: Path): Rule[JsValue, JsValue] =
    Rule[JsValue, JsValue] { json =>
      pathToJsPath(p)(json) match {
        case Nil => Failure(Seq(Path -> Seq(ValidationError("validation.required"))))
        case js :: _ => Success(js)
      }
    }
}
```

Now we are able to do this:

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
	(__ \ "field2").read[JsValue]
}

pick.validate(js) // Success(JsNumber(123))
```

Which is nice, but is would be much more convenient if we could extract that value as an `Int`.

One solution is to write the following method:

```scala
implicit def pickIntInJson[O](p: Path): Rule[JsValue, JsValue] = ???
```

But we would end up copying 90% of the code we already wrote.
Instead of doing so, we're going to make `pickInJson` a bit smarter by adding an implicit parameter

### Seq
### Option

### Lazyness

## Writes

### Monoid

### Covariance

## Testing