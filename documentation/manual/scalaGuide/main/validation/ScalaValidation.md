# The Play data validation library Basics

## Overview

The Play validation API aims to provide a comprehensive toolkit to parse data of any format, and validate it against user defined rules.

It's also a unification of the Forn Validation API, and the Json validation API.
Being based on the same concepts that the Json validation API available in previous versions, it should feel very similar to any developper already working with the Json API. The validation API is, rather than a totally new design, a simple generalisation of those concepts.

## Design

The validation API is designed around a core defined in package `play.api.data.mapping`, and "extensions", each providing primitives to validate and serialize data from / to a particular format (Json, form encoded request body, etc.). See [[TODO: link | ScalaValidation]] for more informations on this topic.

## A simple example

The API is designed around the concept of `Rule`. A Rule defines a way to validate data.
It's basically a function `I => Validation[O]`, where `I` is the type of the input to validate, and `O` is the expected output type.

Let's say you want to coerce a `String` into an `Float`.
All you need to do is to define a `Rule` from String to Float:

```scala
import play.api.data.mapping._
def isFloat: Rule[String, Float] = ???
```
When a `String` is parsed into an `Float`, two scenario are possible, either:

- The `String` can be parsed as a `Float`.
- The `String` can NOT be parsed as a `Float`

In a typicall scala application, you would use `Float.parseFloat` to parse a `String`. On an "invalid" value, this method throws a `NumberFormatException`.

When validating data, we'd certainly prefer to avoid exceptions, as the failure case is expected to happen quite often.

Furthermore, your application should handle it properly, for example by sending a nice error message to the end user. The execution flow of the application should not be altered by a parsing failure, but rather be part of the process. Exceptions are definitely not the appropriate tool for the job.

Back, to our `Rule`. For now we'll not implement `isFloat`, actually the validation API comes with a number of built-in Rules, including the `Float` parsing `Rule[String, Float]`.

All you have to do is import the default Rules.

```scala
scala> import play.api.data.mapping.Rules
import play.api.data.mapping.Rules

scala> :t Rules.float
play.api.data.mapping.Rule[String,Float]
```

Let's now test it against different String values:

```scala
scala> Rules.float.validate("1")
res1: play.api.data.mapping.VA[String,Float] = Success(1.0)

scala> Rules.float.validate("-13.7")
res2: play.api.data.mapping.VA[String,Float] = Success(-13.7)

scala> Rules.float.validate("abc")
res3: play.api.data.mapping.VA[String,Float] =
  Failure(List((/,List(ValidationError(validation.type-mismatch,WrappedArray(Float))))))
```

> `Rule` is typesafe. You can't apply a `Rule` on an unsuported type, the compiler won't let you:
>
```scala
scala> :t Rules.float
play.api.data.mapping.Rule[String,Float]
```
```scala
scala> Rules.float.validate(Seq(32))
<console>:9: error: type mismatch;
 found   : Seq[Int]
 required: String
              Rules.float.validate(Seq(32))
                                      ^
```

As you already noticed, "abc" is not a valid `Float`, but no exception was thrown. Instead of relying on exceptions, `validate` is returning an object of type `Validation` (here `VA` is just a fancy alias for a special type of validation).

`Validation` represents possible outcomes of Rule application, it can be either :

- A `Success`, containing the value you just validated
  When we use `Rule.float` on "1", since "1" is a valid representation of a `Float`, it returns `Success(1.0)`
- A `Failure`, containing all the errors.
  When we use `Rule.float` on "abc", since "abc" is *not* a valid representation of a `Float`, it returns `Failure(List((/,List(ValidationError(validation.type-mismatch,WrappedArray(Float))))))`. That `Failure` tells us all there is to know: it give us a nice message explaining what has failed, and even gives us a parameter `"Float"`, indicating the type the `Rule` is expecting to find.

> Note that `Validation` is a parameterized type. Just like `Rule`, it keeps track of the input and output types.
The method `validate` of a `Rule[I, O]` always return a `VA[I, O]`

## Defining your own Rules

Creating a new Rule is almost as simple as creating a new function.
This example creates a new `Rule` trying to get the first element of a `List[Int]`.

Of course, it must handle the case of an empty `List[Int]`.

All there is to do it to pass a function `I => Validation[I, O]` to `Rule.fromMapping`:

```scala
import play.api.data.mapping._

val headInt: Rule[List[Int], Int] = Rule.fromMapping {
  case Nil => Failure(Seq(ValidationError("validation.emptyList")))
  case head :: _ => Success(head)
}
```

```scala
scala> headInt.validate(List(1, 2, 3, 4, 5))
res1: play.api.data.mapping.VA[List[Int],Int] = Success(1)

scala> headInt.validate(Nil)
res2: play.api.data.mapping.VA[List[Int],Int] =
  Failure(List((/,List(ValidationError(validation.emptyList,WrappedArray())))))
```

We can make this rule a bit more generic:

```scala
import play.api.data.mapping._

def head[T]: Rule[List[T], T] = Rule.fromMapping {
  case Nil => Failure(Seq(ValidationError("validation.emptyList")))
  case head :: _ => Success(head)
}
```

```scala
scala> head.validate(List('a', 'b', 'c', 'd'))
res1: play.api.data.mapping.VA[List[Char],Char] = Success(a)

scala> head.validate(List[Char]())
res2: play.api.data.mapping.VA[List[Char],Char] =
  Failure(List((/,List(ValidationError(validation.emptyList,WrappedArray())))))
```

## Composing Rules

So far we've defined very simple Rules. Of course most "real world" applications require more complex validations.
Let's say we want to write a Rule that given a List of Strings, take the first String in that List, and try to parse It as a Float.

We already have defined:

1. A `Rule[List[T], T]` that returns the first element of a `List`
2. A `Rule[String, Float]` that parses a `String` into a `Float`

We've done almost all the work already. We just have to create a new `Rule` the applies the first `Rule` and if it return a `Success`, apply the second `Rule`.

It would be fairly easy to create such a `Rule` "manually", but we don't have to. A method doing just that is already available:

```scala
scala> val firstFloat = head compose Rules.float
firstFloat: play.api.data.mapping.Rule[List[String],Float] = play.api.data.mapping.Rule$$anon$2@33ea649e
```

```scala
scala> firstFloat.validate(List("1", "2"))
res1: play.api.data.mapping.VA[List[String],Float] = Success(1.0)

scala> firstFloat.validate(List("1.2", "foo"))
res2: play.api.data.mapping.VA[List[String],Float] = Success(1.2)
```

If the list is empty, we get the error from `head`

```scala
scala> firstFloat.validate(List())
res4: play.api.data.mapping.VA[List[String],Float] =
  Failure(List((/,List(ValidationError(validation.emptyList,WrappedArray())))))
```

If the first element is not parseable, we get the error from `Rules.float`.

```scala
scala> firstFloat.validate(List("foo", "2"))
res3: play.api.data.mapping.VA[List[String],Float] =
  Failure(List((/,List(ValidationError(validation.type-mismatch,WrappedArray(Float))))))
```

Of course everything is still typesafe:

```scala
scala> firstFloat.validate(List(1, 2, 3))
<console>:20: error: type mismatch;
 found   : Int(1)
 required: String
              firstFloat.validate(List(1, 2, 3))
                                       ^
```

### Improving reporting.

All is fine with our new `Rule` but the error reporting when we parse an element is not perfect yet.
When a parsing error happens, the `Failure` does not tells us that it happened on the first element of the `List`.

To fix that, we can pass  an additionnal parameter to `compose`:

```scala
scala> val firstFloat2 = head.compose(Path \ 0)(Rules.float)
firstFloat2: play.api.data.mapping.Rule[List[String],Float] = play.api.data.mapping.Rule$$anon$2@2e083a3a

scala> firstFloat2.validate(List("foo", "2"))
res19: play.api.data.mapping.VA[List[String],Float] = Failure(List(([0],List(ValidationError(validation.type-mismatch,WrappedArray(Float))))))
```


## Working with `Validation`

```scala
def demo = Action(parse.json) { request =>
  import play.api.data.mapping._
  import play.api.data.mapping.json.Rules._

  val json = request.body
  val findFriend = (Path \ "user" \ "friend").read[JsValue, JsValue]
  val validated = findFriend.validate(json)

  validated.fold(
    errors => BadRequest,
    friend => Ok(friend)
  )
}
```
