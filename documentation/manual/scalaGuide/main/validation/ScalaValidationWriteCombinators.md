# Combining Writes

## Introduction

We've already explained what a `Write` is in [[the previous chapter | ScalaValidationWrite]]. Those examples were only covering simple writes. Most of the time, writes are used to transform complex hierarchical objects, like [[Json|ScalaValidationJson]], or [[Forms|ScalaValidationJson]].

In the validation API, we create complex object writes by combining simple writes. This chapter details the creation of those complex writes.

> All the examples below are transforming classes to Json objects. The API is not dedicated only to Json, it can be used on any type. Please refer to [[Validating Json | ScalaValidationJson]], [[Validating Forms|ScalaValidationJson]], and [[Supporting new types|ScalaValidationExtension]] for more informations.

## Path

### Extracting data using `Path`

#### The `write` method

> By convention, all useful serialization methods for a given type are to be found in an object called `Writes`. That object contains a bunch of implicits defining how to serialize primitives scala types into the expected output types.

### Type coercion

### Full example

```scala
import play.api.libs.json.Json
import play.api.data.mapping._
import play.api.data.mapping.json.Rules._

val js = Json.parse("""{
  "user": {
    "name" : "toto",
    "age" : 25,
    "email" : "toto@jmail.com",
    "isAlive" : true,
    "friend" : {
  	  "name" : "tata",
  	  "age" : 20,
  	  "email" : "tata@coldmail.com"
    }
  }
}""")

val age = (Path \ "user" \ "age").from[JsValue](min(0) |+| max(130))
age.validate(js) // Success(25)
```

## Combining Writes

```scala
case class User(
	name: String,
	age: Int,
	email: Option[String],
	isAlive: Boolean)
```

We need to create a `Write[User, JsValue]`. Creating this Rule is simply a matter of combining together the writes serializing each field of the class.

```scala
import play.api.libs.json._
import play.api.data.mapping._

val userRule = To[JsValue] { __ =>
	import play.api.data.mapping.json.Writes._
	((__ \ "name").write[String] and
	 (__ \ "age").write[Int] and
	 (__ \ "email").write[Option[String]] and
	 (__ \ "isAlive").write[Boolean])(unlift(User.unapply _))
}
```

> **Important:** Note that we're importing `Writes._` **inside** the `To[I]{...}` block.
It is recommended to always follow this pattern, as it nicely scopes the implicits, avoiding conflicts and accidental shadowing.

`To[JsValue]` defines the `O` type of the writes we're combining. We could have written:

```scala
 (Path \ "name").write[String, JsValue] and
 (Path \ "age").write[Int, JsValue] and
 //...
```

but repeating `JsValue` all over the place is just not very DRY.

> **Next:** - [[Macro Inception | ScalaValidationMacros]]
> **For more examples and snippets:** - [[Cookbook | ScalaValidationCookbook]]