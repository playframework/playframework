<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# JSON Reads/Writes/Format Combinators

[[JSON basics|ScalaJson]] introduced [`Reads`](api/scala/index.html#play.api.libs.json.Reads) and [`Writes`](api/scala/index.html#play.api.libs.json.Writes) converters which are used to convert between [`JsValue`](api/scala/index.html#play.api.libs.json.JsValue) structures and other data types. This page covers in greater detail how to build these converters and how to use validation during conversion.

The examples on this page will use this `JsValue` structure and corresponding model:

@[sample-json](code/ScalaJsonCombinatorsSpec.scala)

@[sample-model](code/ScalaJsonCombinatorsSpec.scala)

## JsPath

[`JsPath`](api/scala/index.html#play.api.libs.json.JsPath) is a core building block for creating `Reads`/`Writes`. `JsPath` represents the location of data in a `JsValue` structure. You can use the `JsPath` object (root path) to define a `JsPath` child instance by using syntax similar to traversing `JsValue`:

@[jspath-define](code/ScalaJsonCombinatorsSpec.scala)

The [`play.api.libs.json`](api/scala/index.html#play.api.libs.json.package) package defines an alias for `JsPath`: `__` (double underscore). You can use this if you prefer:

@[jspath-define-alias](code/ScalaJsonCombinatorsSpec.scala)

## Reads
[`Reads`](api/scala/index.html#play.api.libs.json.Reads) converters are used to convert from a `JsValue` to another type. You can combine and nest `Reads` to create more complex `Reads`.

You will require these imports to create `Reads`:

@[reads-imports](code/ScalaJsonCombinatorsSpec.scala)

### Path Reads
`JsPath` has methods to create special `Reads` that apply another `Reads` to a `JsValue` at a specified path:

- `JsPath.read[T](implicit r: Reads[T]): Reads[T]` - Creates a `Reads[T]` that will apply the implicit argument `r` to the `JsValue` at this path.
- `JsPath.readNullable[T](implicit r: Reads[T]): Reads[Option[T]]readNullable` - Use for paths that may be missing or can contain a null value.

> Note: The JSON library provides implicit `Reads` for basic types such as String, Int, Double, etc.

Defining an individual path `Reads` looks like this:

@[reads-simple](code/ScalaJsonCombinatorsSpec.scala)

### Complex Reads
You can combine individual path `Reads` to form more complex `Reads` which can be used to convert to complex models.

For easier understanding, we'll break down the combine functionality into two statements. First combine `Reads` objects using the `and` combinator:

@[reads-complex-builder](code/ScalaJsonCombinatorsSpec.scala)

This will yield a type of `FunctionalBuilder[Reads]#CanBuild2[Double, Double]`. This is an intermediary object and you don't need to worry too much about it, just know that it's used to create a complex `Reads`. 

Second call the `apply` method of `CanBuildX` with a function to translate individual values to your model, this will return your complex `Reads`. If you have a case class with a matching constructor signature, you can just use its `apply` method:

@[reads-complex-buildertoreads](code/ScalaJsonCombinatorsSpec.scala)

Here's the same code in a single statement:

@[reads-complex-statement](code/ScalaJsonCombinatorsSpec.scala)

### Validation with Reads

The `JsValue.validate` method was introduced in [[JSON basics|ScalaJson]] as the preferred way to perform validation and conversion from a `JsValue` to another type. Here's the basic pattern:

@[reads-validation-simple](code/ScalaJsonCombinatorsSpec.scala)

Default validation for `Reads` is minimal, such as checking for type conversion errors. You can define custom validation rules by using `Reads` validation helpers. Here are some that are commonly used: 

- `Reads.email` - Validates a String has email format.  
- `Reads.minLength(nb)` - Validates the minimum length of a String.
- `Reads.min` - Validates a minimum numeric value.
- `Reads.max` - Validates a maximum numeric value.
- `Reads[A] keepAnd Reads[B] => Reads[A]` - Operator that tries `Reads[A]` and `Reads[B]` but only keeps the result of `Reads[A]` (For those who know Scala parser combinators `keepAnd == <~` ).
- `Reads[A] andKeep Reads[B] => Reads[B]` - Operator that tries `Reads[A]` and `Reads[B]` but only keeps the result of `Reads[B]` (For those who know Scala parser combinators `andKeep == ~>` ).
- `Reads[A] or Reads[B] => Reads` - Operator that performs a logical OR and keeps the result of the last Reads checked.

To add validation, apply helpers as arguments to the `JsPath.read` method:

@[reads-validation-custom](code/ScalaJsonCombinatorsSpec.scala)

### Putting it all together

By using complex `Reads` and custom validation we can define a set of effective `Reads` for our example model and apply them:

@[reads-model](code/ScalaJsonCombinatorsSpec.scala)

Note that complex `Reads` can be nested. In this case, `placeReads` uses the previously defined implicit `locationReads` and `residentReads` at specific paths in the structure.

## Writes
[`Writes`](api/scala/index.html#play.api.libs.json.Writes) converters are used to convert from some type to a `JsValue`.

You can build complex `Writes` using `JsPath` and combinators very similar to `Reads`. Here's the `Writes` for our example model:

@[writes-model](code/ScalaJsonCombinatorsSpec.scala)

There are a few differences between complex `Writes` and `Reads`:

- The individual path `Writes` are created using the `JsPath.write` method.
- There is no validation on conversion to `JsValue` which makes the structure simpler and you won't need any validation helpers.
- The intermediary `FunctionalBuilder#CanBuildX` (created by `and` combinators) takes a function that translates a complex type `T` to a tuple matching the individual path `Writes`. Although this is symmetrical to the `Reads` case, the `unapply` method of a case class returns an `Option` of a tuple of properties and must be used with `unlift` to extract the tuple.

## Recursive Types
One special case that our example model doesn't demonstrate is how to handle `Reads` and `Writes` for recursive types. `JsPath` provides `lazyRead` and `lazyWrite` methods that take call-by-name parameters to handle this:

@[reads-writes-recursive](code/ScalaJsonCombinatorsSpec.scala)

## Format
[`Format[T]`](api/scala/index.html#play.api.libs.json.Format) is just a mix of the `Reads` and `Writes` traits and can be used for implicit conversion in place of its components.

### Creating Format from Reads and Writes
You can define a `Format` by constructing it from `Reads` and `Writes` of the same type:

@[format-components](code/ScalaJsonCombinatorsSpec.scala)

### Creating Format using combinators
In the case where your `Reads` and `Writes` are symmetrical (which may not be the case in real applications), you can define a `Format` directly from combinators:

@[format-combinators](code/ScalaJsonCombinatorsSpec.scala)
