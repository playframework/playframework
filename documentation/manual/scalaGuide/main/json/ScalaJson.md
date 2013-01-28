# The Play JSON library Basics

## Overview

The recommended way of dealing with JSON is using Play’s typeclass based JSON library, located at ```play.api.libs.json```. 

For parsing JSON strings, Play uses super-fast Java based JSON library, [Jackson](http://jackson.codehaus.org/).  

The benefit of this approach is that both the Java and the Scala side of Play can share the same underlying library (Jackson), while Scala users can enjoy the extra type safety and functional aspects that Play’s JSON support brings to the table.

## JSON is an AST (_Abstract Syntax Tree_)

Take JSON example:

```json
{ 
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
}
```

This can be seen as a tree structure using the 2 following structures:

- **JSON object** contains a set of `name` / `value` pairs:
    - `name` is a String
    - `value` can be :
        - string
        - number
        - another JSON object
        - a JSON array
        - true/false
        - null
- **JSON array** is a sequence of values from the previously listed value types.

> If you want to have more info about the exact JSON standard, please go to [json.org](http://json.org/)


## Json Data Types

`play.api.libs.json` package contains 7 JSON data types reflecting exactly the previous structure.

### ```JsObject``` 

- This is a set of name/value pairs as described in standard.
- `{ "name" : "toto", "age" : 45 }` as a JSON example.

### ```JsNull```

This represents `null` value in JSON

### ```JsBoolean```

This is a boolean with value `true` or `false`

### ```JsNumber```

- JSON does NOT discriminate `short`, `int`, `long`, `float`, `double`, `bigdecimal` so it is represented by `JsNumber` containing a `bigdecimal`. 
- Play JSON API brings more type precision when converting to Scala structures.

### ```JsArray``` 

- An array is a sequence of any Json value types (not necessarily the same type).
- `[ "alpha", "beta", true, 123.44, 334]` as a JSON example.

### ```JsString```

A classic String.

### ```JsUndefined```

This is not part of the JSON standard and is only used internally by the API to represent some error nodes in the AST.

### ```JsValue```

All previous types inherit from the generic JSON trait, ```JsValue```.

## Minimal Import to work with basic JSON API

```scala
import play.api.libs.json.Json
```

This import give access to the most basic JSON features :

- `Json.parse` : parses a string to JsValue
- `Json.stringify` : stringifies a JsValue using compact printer (NO line feed/indentation)
- `Json.prettyPrint` : stringifies a JsValue using pretty printer (line feed + indentation)
- `Json.toJson[T](t: T)(implicit writes: Writes[T])` : tries to convert a Scala structure to a `JsValue` using the resolved implicit `Writes[T]`
- `Json.fromJson[T](json: JsValue)(implicit reads: Reads[T])` : tries to convert a `JsValue` to a Scala structure using the resolved implicit `Reads[T]`
- `Json.obj()` : simplified syntax to create a `JsObject`
- `Json.arr()` : simplified syntax to create a `JsArray`


## Parsing a JSON String

You can easily parse any JSON string as a `JsValue`:

```
import play.api.libs.json.Json

val json: JsValue = Json.parse("""
{ 
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
}
""")
```

This sample is used in all next samples.

As explained previously, the parsing is performed by [Jackson](http://jackson.codehaus.org/).

## Constructing JSON directly

### Raw way

The previous sample Json object can be created in other ways too. 
Here is the raw approach.

```
import play.api.libs.json._

JsObject(
  "users" -> JsArray(
    JsObject(
      "name" -> JsString("Bob") ::
      "age" -> JsNumber(31) ::
      "email" -> JsString("bob@gmail.com") ::
      Nil) ::
    JsObject(
      "name" -> JsString("Kiki") ::
      "age" -> JsNumber(25) ::
      "email" -> JsNull ::
      Nil
    ) :: Nil
  ) :: Nil
)
```

### Preferred way

Play now provides a simplified syntax to build your JSON.
The previous JsObject can be constructed as following:

```
import play.api.libs.json.Json

Json.obj(
  "users" -> Json.arr(
    Json.obj(
      "name" -> "bob",
      "age" -> 31,
      "email" -> "bob@gmail.com"  	  
    ),
    Json.obj(
      "name" -> "kiki",
      "age" -> 25,
      "email" -> JsNull  	  
    )
  )
)
```


## Serializing JSON

Serializing a `JsValue` to its JSON String representation is easy:

```
import play.api.libs.json.Json

val jsonString: String = Json.stringify(jsValue)
```


## Accessing Path in a JSON tree 

As soon as you have a `JsValue` you can navigate into the JSON tree.  
The API looks like the one provided to navigate into XML document by Scala using `NodeSeq` except you retrieve `JsValue`.

### Simple path `\`

```scala
// Here we import everything under json in case we need to manipulate different Json types
scala> import play.api.libs.json._

scala> val name: JsValue = json \ "user" \ "name"
name: play.api.libs.json.JsValue = "toto"
```

### Recursive path `\\`
 
```scala
// recursively searches in the sub-tree and returns a Seq[JsValue]
// of all found JsValue
scala> val emails: Seq[String] = json \ "user" \\ "email"
emails: Seq[play.api.libs.json.JsValue] = List("toto@jmail.com", "tata@coldmail.com")
```
 

## Converting JsValue to Scala Value

While navigating JSON tree, you retrieve `JsValue` but you may want to convert the JsValue to a Scala type. 
For ex, a `JsString` to a `String` or a `JsNumber` to a `Long` (if it can be converted).

### Unsafe conversion with `json.as[T]`

`as[T]` is unsafe because it tries to access the path and to convert to the required type. But if the path is not found or the conversion not possible, it generates a `JsResultException` RuntimeException containing detected errors.

#### case OK: path found & conversion possible

```scala
// returns the value converted to provided type (if possible and if found)
scala> val name: String = (json \ "user" \ "name").as[String]
name: String = toto
```

#### case KO: Path not found

```scala
scala> val nameXXX: String = (json \ "user" \ "nameXXX").as[String]
play.api.libs.json.JsResultException: JsResultException(errors:List((,List(ValidationError(validate.error.expected.jsstring,WrappedArray())))))
	at play.api.libs.json.JsValue$$anonfun$4.apply(JsValue.scala:65)
	at play.api.libs.json.JsValue$$anonfun$4.apply(JsValue.scala:65)
	at play.api.libs.json.JsResult$class.fold(JsResult.scala:69)
	at play.api.libs.json.JsError.fold(JsResult.scala:10)
	at play.api.libs.json.JsValue$class.as(JsValue.scala:63)
	at play.api.libs.json.JsUndefined.as(JsValue.scala:96)
```

> Please note the error that doesn't return `path.not.found` as you may expect. This is a difference from JSON combinators presented later in the doc. 
> This is due to the fact that `(json \ "user" \ "nameXXX")` returns `JsNull` and the implicit `Reads[String]` here awaits a `JsString` which explains the detected error.


#### case KO: Conversion not possible

```scala
scala> val name: Long = (json \ "user" \ "name").as[Long]
play.api.libs.json.JsResultException: JsResultException(errors:List((,List(ValidationError(validate.error.expected.jsnumber,WrappedArray())))))
	at play.api.libs.json.JsValue$$anonfun$4.apply(JsValue.scala:65)
	at play.api.libs.json.JsValue$$anonfun$4.apply(JsValue.scala:65)
	at play.api.libs.json.JsResult$class.fold(JsResult.scala:69)
	at play.api.libs.json.JsError.fold(JsResult.scala:10)
	at play.api.libs.json.JsValue$class.as(JsValue.scala:63)
	at play.api.libs.json.JsString.as(JsValue.scala:111)
```

<br/>
### Safer conversion with `Option[T]`

`as[T]` is immediate but not robust so there is `asOpt[T]` which returns None in case of error of any type.

#### case OK: path found & conversion possible

```scala
scala> val maybeName: Option[String] = (json \ "user" \ "name").asOpt[String]
maybeName: Option[String] = Some(toto)
```

#### case KO: Path not found

```scala
scala> val maybeNameXXX: Option[String] = (json \ "user" \ "nameXXX").asOpt[String]
maybeNameXXX: Option[String] = None
```

#### case KO: Conversion not possible

```scala
scala> val maybeNameLong: Option[Long] = (json \ "user" \ "name").asOpt[Long]
maybeNameLong: Option[Long] = None
```

<br/>
### Safest conversion with `validate[T]`

`asOpt[T]` is better but you lose the kind of error that was detected.  

`validate[T]` is there to provide the safest and most robust way to convert a `JsValue` by returning a `JsResult[T]`:

- `JsResult[T]` accumulates all detected errors (doesn't stop at 1st error),
- `JsResult[T]` is a monadic structure providing `map`/`flatMap`/`fold` operations to manipulate, compose it.

#### `JsResult[T]` in a very nutshell

`JsResult[T]` can have 2 values:

- `JsSuccess[T](value: T, path: JsPath = JsPath())` contains: 
    - `value: T` when conversion was OK,
    - FYI, don't focus on `path` which is mainly an internal field used by the API to represent the current traversed `JsPath`.

> Please note : `JsPath` will be described later but it is just the same as `XMLPath` for JSON. 
> When you write : 
> `json \ "user" \ "name"`
> It can be written as following : 
> `(JsPath \ "user" \ "name")(json)`
> _You create a `JsPath` to search `user` then `name` and apply it to a given `json`._


- `JsError(errors: Seq[(JsPath, Seq[ValidationError])])` :  
    - `errors` is a Sequence of pairs `(JsPath, Seq[ValidationError])`
    - pair `(JsPath, Seq[ValidationError])` locates one or more detected errors at given `JsPath`

A few samples of usage:

```scala
scala> import play.api.libs.json._

scala> val jsres: JsResult[String] = JsString("toto").validate[String]
jsres: JsSuccess("toto")

scala> val jsres: JsResult[String] = JsNumber(123).validate[String]
jsres: play.api.libs.json.JsResult[String] = JsError(List((,List(ValidationError(validate.error.expected.jsstring,WrappedArray())))))

jsres.map{ s: String => …}
jsres.flatMap{ s: String => JsSuccess(s) }

jsres.fold( 
  errors: Seq[(JsPath, Seq[ValidationError])] => // manage errors,
  s: String => // manage value 
)

jsres.map( s: String => // manage value )
     .recoverTotal( jserror: JsError => // manage errors and return default value)
```

#### case OK: path found & conversion possible

```scala
scala> val safeName = (json \ "user" \ "name").validate[String]
safeName: play.api.libs.json.JsResult[String] = JsSuccess(toto,) // path is not precised because it's root
```

#### case KO: Path not found

```scala
scala> val nameXXX = (json \ "user" \ "nameXXX").validate[String]
nameXXX: play.api.libs.json.JsResult[String] = 
  JsError(List((,List(ValidationError(validate.error.expected.jsstring,WrappedArray())))))
```

> Please note the error that doesn't return `path.not.found` as you may expect. This is a difference from JSON combinators presented later in the doc. 
> This is due to the fact that `(json \ "user" \ "nameXXX")` returns `JsNull` and the implicit `Reads[String]` here awaits a `JsString` which explains the detected error.

#### case KO: Conversion not possible

```scala
scala> val name = (json \ "user" \ "name").validate[Long]
name: play.api.libs.json.JsResult[Long] = 
  JsError(List((,List(ValidationError(validate.error.expected.jsnumber,WrappedArray())))))
```

<br/>
### Converting Recursive path `\\`
 
`\\` recursively searches in the sub-tree and returns a `Seq[JsValue]` of found JsValue which is then a collection with classical Scala functions.

```scala
scala> val emails: Seq[String] = (json \ "user" \\ "email").map(_.as[String])
emails: Seq[String] = List(toto@jmail.com, tata@coldmail.com)
```
<br/>
## Converting a Scala value to JsValue

Scala to JSON conversion is performed by function `Json.toJson[T](implicit writes: Writes[T])` based on implicit typeclass `Writes[T]` which is just able to convert a `T` to a `JsValue`. 

### Create very simple JsValue

```
val jsonNumber = Json.toJson(4)
jsonNumber: play.api.libs.json.JsValue = 4
```

*This conversion is possible because Play JSON API provides an implicit `Writes[Int]`*


### Create a JSON array from a Seq[T]

```
import play.api.libs.json.Json

val jsonArray = Json.toJson(Seq(1, 2, 3, 4))
jsonArray: play.api.libs.json.JsValue = [1,2,3,4]
```

*This conversion is possible because Play JSON API provides an implicit `Writes[Seq[Int]]`*

Here we have no problem to convert a `Seq[Int]` into a Json array. However it is more complicated if the `Seq` contains heterogeneous values:

```
import play.api.libs.json.Json

val jsonArray = Json.toJson(Seq(1, "Bob", 3, 4))
<console>:11: error: No Json deserializer found for type Seq[Any]. Try to implement an implicit Writes or Format for this type.
       val jsonArray = Json.toJson(Seq(1, "Bob", 3, 4))
```

You get an error because there is no way to convert a `Seq[Any]` to Json (`Any` could be anything including something not supported by Json right?)

A simple solution is to handle it as a `Seq[JsValue]`:

```
import play.api.libs.json.Json

val jsonArray = Json.toJson(Seq(
  toJson(1), toJson("Bob"), toJson(3), toJson(4)
))
```
*This conversion is possible because Play API JSON provides an implicit `Writes[Seq[JsValue]]`*

### Create a JSON object from a Map[String, T]

```
import play.api.libs.json.Json

val jsonObject = Json.toJson(
  Map(
    "users" -> Seq(
      toJson(
        Map(
          "name" -> toJson("Bob"),
          "age" -> toJson(31),
          "email" -> toJson("bob@gmail.com")
        )
      ),
      toJson(
        Map(
          "name" -> toJson("Kiki"),
          "age" -> toJson(25),
          "email" -> JsNull
        )
      )
    )
  )
)
```

That will generate this Json result:

```
{
  "users":[
    {
      "name": "Bob",
      "age": 31.0,
      "email": "bob@gmail.com"
    },
    {
      "name": "Kiki",
      "age":  25.0,
      "email": null
    }
  ]
}
```

> **Next:** [[JSON Reads/Writes/Formats Combinators | ScalaJsonCombinators]]