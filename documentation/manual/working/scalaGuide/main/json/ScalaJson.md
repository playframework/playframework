<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# JSON basics

Modern web applications often need to parse and generate data in the JSON (JavaScript Object Notation) format. Play supports this via its [JSON library](api/scala/index.html#play.api.libs.json.package).

JSON is a lightweight data-interchange format and looks like this:

```json
{
  "name" : "Watership Down",
  "location" : {
    "lat" : 51.235685,
    "long" : -1.309197
  },
  "residents" : [ {
    "name" : "Fiver",
    "age" : 4,
    "role" : null
  }, {
    "name" : "Bigwig",
    "age" : 6,
    "role" : "Owsla"
  } ]
}
```

> To learn more about JSON, see [json.org](http://json.org/).

## The Play JSON library
The [`play.api.libs.json`](api/scala/index.html#play.api.libs.json.package) package contains data structures for representing JSON data
and utilities for converting between these data structures and other data representations. Types of interest are:

### [`JsValue`](api/scala/index.html#play.api.libs.json.JsValue)

This is a trait representing any JSON value. The JSON library has a case class extending `JsValue` to represent each valid JSON type:

- [`JsString`](api/scala/index.html#play.api.libs.json.JsString)
- [`JsNumber`](api/scala/index.html#play.api.libs.json.JsNumber)
- [`JsBoolean`](api/scala/index.html#play.api.libs.json.JsBoolean)
- [`JsObject`](api/scala/index.html#play.api.libs.json.JsObject)
- [`JsArray`](api/scala/index.html#play.api.libs.json.JsArray)
- [`JsNull`](api/scala/index.html#play.api.libs.json.JsNull)

Using the various JSValue types, you can construct a representation of any JSON structure.

### [`Json`](api/scala/index.html#play.api.libs.json.Json$)
The Json object provides utilities, primarily for conversion to and from JsValue structures.

### [`JsPath`](api/scala/index.html#play.api.libs.json.JsPath)
Represents a path into a JSValue structure, analogous to XPath for XML. This is used for traversing JsValue structures and in patterns for implicit converters.

## Converting to a JsValue

### Using string parsing

@[convert-from-string](code/ScalaJsonSpec.scala)

### Using class construction

@[convert-from-classes](code/ScalaJsonSpec.scala)

`Json.obj` and `Json.arr` can simplify construction a bit. Note that most values don't need to be explicitly wrapped by JsValue classes, the factory methods use implicit conversion (more on this below).

@[convert-from-factory](code/ScalaJsonSpec.scala)

### Using Writes converters
Scala to JsValue conversion is performed by the utility method `Json.toJson[T](T)(implicit writes: Writes[T])`. This functionality depends on a converter of type [`Writes[T]`](api/scala/index.html#play.api.libs.json.Writes) which can convert a `T` to a `JsValue`. 

The Play JSON API provides implicit `Writes` for most basic types, such as `Int`, `Double`, `String`, and `Boolean`. It also supports `Writes` for collections of any type `T` that a `Writes[T]` exists. 

@[convert-from-simple](code/ScalaJsonSpec.scala)

To convert your own models to JsValues, you must define implicit `Writes` converters and provide them in scope.

@[sample-model](code/ScalaJsonSpec.scala)

@[convert-from-model](code/ScalaJsonSpec.scala)

Alternatively, you can define your `Writes` using the combinator pattern:

> Note: The combinator pattern is covered in detail in [[JSON Reads/Writes/Formats Combinators|ScalaJsonCombinators]].

@[convert-from-model-prefwrites](code/ScalaJsonSpec.scala)

## Traversing a JsValue structure

You can traverse a `JsValue` structure and extract specific values. The syntax and functionality is similar to Scala XML processing.

> Note: The following examples are applied to the JsValue structure created in previous examples.

### Simple path `\`
Applying the `\` operator to a `JsValue` will return the property corresponding to the field argument, supposing this is a JsObject. 

@[traverse-simple-path](code/ScalaJsonSpec.scala)

### Recursive path `\\`
Applying the `\\` operator will do a lookup for the field in the current object and all descendants.

@[traverse-recursive-path](code/ScalaJsonSpec.scala)

### Index lookup (for JsArrays)
You can retrieve a value in a `JsArray` using an apply operator with the index number.

@[traverse-array-index](code/ScalaJsonSpec.scala)

## Converting from a JsValue

### Using String utilities
Minified:

@[convert-to-string](code/ScalaJsonSpec.scala)

```
{"name":"Watership Down","location":{"lat":51.235685,"long":-1.309197},"residents":[{"name":"Fiver","age":4,"role":null},{"name":"Bigwig","age":6,"role":"Owsla"}]}
```
Readable:

@[convert-to-string-pretty](code/ScalaJsonSpec.scala)

```
{
  "name" : "Watership Down",
  "location" : {
    "lat" : 51.235685,
    "long" : -1.309197
  },
  "residents" : [ {
    "name" : "Fiver",
    "age" : 4,
    "role" : null
  }, {
    "name" : "Bigwig",
    "age" : 6,
    "role" : "Owsla"
  } ]
}
```

### Using JsValue.as/asOpt

The simplest way to convert a `JsValue` to another type is using `JsValue.as[T](implicit fjs: Reads[T]): T`. This requires an implicit converter of type [`Reads[T]`](api/scala/index.html#play.api.libs.json.Reads) to convert a `JsValue` to `T` (the inverse of `Writes[T]`). As with `Writes`, the JSON API provides `Reads` for basic types.

@[convert-to-type-as](code/ScalaJsonSpec.scala)

The `as` method will throw a `JsResultException` if the path is not found or the conversion is not possible. A safer method is `JsValue.asOpt[T](implicit fjs: Reads[T]): Option[T]`.

@[convert-to-type-as-opt](code/ScalaJsonSpec.scala)

Although the `asOpt` method is safer, any error information is lost.

### Using validation
The preferred way to convert from a `JsValue` to another type is by using its `validate` method (which takes an argument of type `Reads`). This performs both validation and conversion, returning a type of [`JsResult`](api/scala/index.html#play.api.libs.json.JsResult). `JsResult` is implemented by two classes:

- [`JsSuccess`](api/scala/index.html#play.api.libs.json.JsSuccess) - Represents a successful validation/conversion and wraps the result.
- [`JsError`](api/scala/index.html#play.api.libs.json.JsError) - Represents unsuccessful validation/conversion and contains a list of validation errors.

You can apply various patterns for handling a validation result:

@[convert-to-type-validate](code/ScalaJsonSpec.scala)

### JsValue to a model

To convert from JsValue to a model, you must define implicit `Reads[T]` where `T` is the type of your model.

> Note: The pattern used to implement `Reads` and custom validation are covered in detail in [[JSON Reads/Writes/Formats Combinators|ScalaJsonCombinators]].

@[sample-model](code/ScalaJsonSpec.scala)

@[convert-to-model](code/ScalaJsonSpec.scala)
