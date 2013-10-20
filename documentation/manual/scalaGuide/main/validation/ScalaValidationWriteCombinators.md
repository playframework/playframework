# Combining Writes

## Introduction

We've already explained what a `Write` is in [[the previous chapter | ScalaValidationWrite]]. Those examples were only covering simple writes. Most of the time, writes are used to transform complex hierarchical objects.

In the validation API, we create complex object writes by combining simple writes. This chapter details the creation of those complex writes.

> All the examples below are transforming classes to Json objects. The API is not dedicated only to Json, it can be used on any type. Please refer to [[Serializing Json | ScalaValidationJson]], [[Serializing Forms|ScalaValidationJson]], and [[Supporting new types|ScalaValidationExtension]] for more informations.

## Path

### Serializing data using `Path`

#### The `write` method

We start by creating a Path representing the location at which we'd like to serialize our data:

```scala
scala> import play.api.data.mapping._
scala> val location: Path = Path \ "user" \ "friend"
location: play.api.data.mapping.Path = /user/friend
```

`Path` has a `write[I, O]` method, where `I` represents the input we’re trying to serialize, and `O` is the output type. For example, `(Path \ "foo").write[Int, JsObject]`, means we want to try to serialize a value of type `Int` into a `JsObject` at `/foo`.

But let's try something much easier for now:

```scala
import play.api.libs.json.JsValue
import play.api.data.mapping._

val location: Path = Path \ "user" \ "friend"
val serializeFriend: Write[JsValue, JsObject] = location.write[JsValue, JsObject]
```

`location.write[JsValue, JsObject]` means the we're trying to serialize a `JsValue` to `location` in a `JsObject`. Effectively, we're just defining a `Write` that is putting a `JsValue` into a `JsObject` at the given location.

If you try to run that code, the compiler gives you the following error:

```scala
<console>:11: error: No implicit view available from play.api.data.mapping.Path => play.api.data.mapping.Write[play.api.libs.json.JsValue,play.api.libs.json.JsObject].
       val serializeFriend: Write[JsValue, JsObject] = location.write[JsValue, JsObject]
                                                                    ^
```

The Scala compiler is complaining about not finding an implicit function of type `Path => Write[JsValue, JsObject]`. Indeed, unlike the Json API, you have to provide a method to **transform** the input type into the output type.

Fortunately, such method already exists. All you have to do is import it:

```scala
scala> import play.api.data.mapping.json.Writes._
import play.api.data.mapping.json.Writes._
```

> By convention, all useful serialization methods for a given type are to be found in an object called `Writes`. That object contains a bunch of implicits defining how to serialize primitives Scala types into the expected output types.

With those implicits in scope, we can finally create our `Write`:

```scala
scala> import play.api.data.mapping.json.Writes._
import play.api.data.mapping.json.Writes._

scala> val serializeFriend: Write[JsValue, JsObject] = location.write[JsValue, JsObject]
serializeFriend: Write[JsValue, JsObject] = play.api.data.mapping.Write$$anon$4@5a370c51
```

Alright, so far we've defined a `Write` looking for some data of type `JsValue`, located at `/user/friend` in a `JsObject`.

Now we need to apply this `Write` on our data:

```scala
import play.api.libs.json._

scala> serializeFriend.writes(JsString("Julien"))
res1: play.api.libs.json.JsValue = {"user":{"friend":"Julien"}}
```

### Type coercion

We now are capable of serializing data to a given `Path`. Let's do it again on a different sub-tree:

```scala
import play.api.data.mapping._
import play.api.data.mapping.json.Writes._

val age = (Path \ "user" \ "age").write[JsValue, JsObject]
```

And if we apply this new `Write`:

```scala
scala> age.writes(JsNumber(28))
res1: play.api.libs.json.JsObject = {"user":{"age":28}}
```

That example is nice, but chances are `age` in not a `JsNumber`, but an `Int`.
All we have to do is to change the input type in our `Write` definition:

```scala
val age = (Path \ "user" \ "age").write[Int, JsObject]
```

And apply it:

```scala
scala> age.writes(28)
res1: play.api.libs.json.JsObject = {"user":{"age":28}}
```

So scala *automagically* figures out how to transform a `Int` into an `JsObject`. How does this happens ?

It's fairly simple. The definition of `write` looks like this:

```scala
def write[I, I](implicit w: Path => Write[I, O]): Write[I, O]
```

So when you use `(Path \ "user" \ "age").write[Int, JsObject]`, the compiler looks for an `implicit Path => Write[Int, JsObject]`, which happens to exist in `play.api.data.mapping.json.Writes`.

### Full example

```scala
import play.api.libs.json.Json
import play.api.data.mapping._
import play.api.data.mapping.json.Writes._

val age = (Path \ "user" \ "age").write[Int, JsObject]
age.writes(28) // {"user":{"age":28}}
```

## Combining Writes

So far we've serialized only primitives types.
Now we'd like to serialize an entire `User` object defined below, and transform it into a `JsObject`:

```scala
case class User(
	name: String,
	age: Int,
	email: Option[String],
	isAlive: Boolean)
```

We need to create a `Write[User, JsValue]`. Creating this `Write` is simply a matter of combining together the writes serializing each field of the class.

```scala
import play.api.libs.json._
import play.api.data.mapping._
import play.api.data.mapping.json.Writes._

val userWrite = To[JsObject] { __ =>
	import play.api.data.mapping.json.Writes._
	((__ \ "name").write[String] and
	 (__ \ "age").write[Int] and
	 (__ \ "email").write[Option[String]] and
	 (__ \ "isAlive").write[Boolean])(unlift(User.unapply _))
}
```

If you try this example, the following compilation error happens:

```scala
<console>:35: error: value and is not a member of play.api.data.mapping.Write[String,play.api.libs.json.JsObject]
       ((__ \ "name").write[String] and
```

Indeed, the `and` method is part of a typeclass defined in `play.api.libs.functional.syntax`.
You need to import `play.api.libs.functional.syntax._`

```scala
import play.api.libs.json._
import play.api.data.mapping._
import play.api.data.mapping.json.Writes._

// DON'T FORGET TO IMPORT THIS
import play.api.libs.functional.syntax._

val userWrite = To[JsObject] { __ =>
  import play.api.data.mapping.json.Writes._
  ((__ \ "name").write[String] and
   (__ \ "age").write[Int] and
   (__ \ "email").write[Option[String]] and
   (__ \ "isAlive").write[Boolean])(unlift(User.unapply _)) // unlift is defined in play.api.libs.functional.syntax
}
```

> **Important:** Note that we're importing `Writes._` **inside** the `To[I]{...}` block.
It is recommended to always follow this pattern, as it nicely scopes the implicits, avoiding conflicts and accidental shadowing.

`To[JsObject]` defines the `O` type of the writes we're combining. We could have written:

```scala
 (Path \ "name").write[String, JsObject] and
 (Path \ "age").write[Int, JsObject] and
 //...
```

but repeating `JsObject` all over the place is just not very DRY.

Let's test it now:

```scala
scala> userWrite.writes(User("Julien", 28, None, true))
res1: play.api.libs.json.JsObject = {"name":"Julien","age":28,"isAlive":true}
```

> **Next:** - [[Macro Inception | ScalaValidationMacros]]
> **For more examples and snippets:** - [[Cookbook | ScalaValidationCookbook]]