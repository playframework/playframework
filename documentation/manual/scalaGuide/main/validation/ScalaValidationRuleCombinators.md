# Combining Rules

## Introduction

We've already explained what a `Rule` is in [[the previous chapter | ScalaValidationRule]]. Those examples were only covering simple rules. Most of the time, rules are used to validate and transform complex hierarchical objects, like [[Json|ScalaValidationJson]], or [[Forms|ScalaValidationJson]].

In the validation API, we create complex object rules by combining simple rules. This chapter details the creation of those complex rules.

> All the examples below are validating Json objects. The API is not dedicated only to Json, it can be used on any type. Please refer to [[Validating Json | ScalaValidationJson]], [[Validating Forms|ScalaValidationJson]], or [[Supporting new types|ScalaValidationExtension]] for more informations.

## Path

The validation API defines a class named `Path`. A `Path` represents a location. Contrarely to `JsPath`, it's not related to any specific type, it's just a location in some data. Most of the time, a `Path` is our entry point into the Validation API.

A `Path` is declared using this syntax:

```scala
scala> import play.api.data.mapping.Path
import play.api.data.mapping.Path

scala> val p = Path \ "foo" \ "bar"
p: play.api.data.mapping.Path = /foo/bar
```

`Path` here is the empty `Path` object. One may call it the root path.

A path can also reference an index:

```scala
scala> val p = Path \ "foo" \ 0
p: play.api.data.mapping.Path = /foo[0]
```

### Extracting data using `Path`

Consider the following json:

```json
import play.api.libs.json.Json

val js: JsValue = Json.parse("""{
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
```

The first step before trying to validate anything is be capable of accessing a fragment of the object.

Assuming you'd like to validate that `friend` exists and is valid in this json, you first need to acess the object located at `user.friend`.

#### The `read` method

We start by creating a `Path` representing the location of the interesting data in our json:

```scala
scala> import play.api.data.mapping._
scala> val location: Path = Path \ "user" \ "friend"
location: play.api.data.mapping.Path = /user/friend
```

`Path` has a `read[I, O]` method. `I` represents the input we're trying to parse, and `O` is the output type. For example, `(Path \ "foo").read[JsValue, Int]`, means we want to try to read a value located at path `/foo` in a `JsValue` as an `Int`.

But let's try something much easier for now:

```scala
import play.api.libs.json.JsValue
import play.api.data.mapping._

val location: Path = Path \ "user" \ "friend"
val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]                                                             ^
```

`location.read[JsValue, JsValue]` means the we're trying to lookup at `location` in a `JsValue`, and we expect to find a `JsValue` there. Effectively, we're just defining a `Rule` that is picking a subtree in a `JsValue`.

If you try to run that code, the compiler gives you the following error:

```
error: No implicit view available from play.api.data.mapping.Path => play.api.data.mapping.Rule[play.api.libs.json.JsValue,play.api.libs.json.JsValue].
       val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]
                                                             ^
```

The Scala compiler is complaining about not finding an implicit function of type `Path => Rule[JsValue, JsValue]`. Indeed, unlike the Json API, you have to provide a method to **lookup** into the data you expect to validate.

Fortunately, such method already exists. All you have to do is import it:

```scala
scala> import play.api.data.mapping.json.Rules._
import play.api.data.mapping.json.Rules._
```

> By convention, all useful validation methods for a given type are to be found in an object called `Rules`. That object contains a bunch of implicits defining how to lookup in the data, and how to coerce some of the possible values of those data into Scala types.

With those implicits in scope, we can finally create our `Rule`:

```scala
scala> import play.api.data.mapping.json.Rules._
import play.api.data.mapping.json.Rules._

scala> val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]
findFriend: Rule[JsValue, JsValue] = play.api.data.mapping.Rule$$anon$2@294d2c21
```

Alright, so far we've defined a `Rule` looking for some data of type `JsValue`, located at `/user/friend` in an object of type `JsValue`.

Now we need to apply this `Rule` to our data:

```scala
scala> findFriend.validate(js)
res1: play.api.data.mapping.VA[JsValue, JsValue] = Success({"name":"tata","age":20,"email":"tata@coldmail.com"})
```

If we can't find anything, applying a `Rule` leads to a `Failure`:

```scala
scala> (Path \ "foobar").read[JsValue, JsValue].validate(js)
res1: VA[JsValue, JsValue] = Failure(List((/foobar,List(ValidationError(validation.required,WrappedArray())))))
```

### Type coercion

We now are capable of extracting data at a given `Path`. Let's do it again on a different sub-tree:

```scala
import play.api.data.mapping._
import play.api.data.mapping.json.Rules._

val age = (Path \ "user" \ "age").read[JsValue, JsValue]
```

And if we apply this new `Rule`:

```scala
scala> age.validate(js)
res1: play.api.data.mapping.VA[JsValue, JsValue] = Success(25)
```

Again, if the json is invalid:

```scala
scala> age.validate(Json.obj())
res5: play.api.data.mapping.VA[JsValue, JsValue] = Failure(List((/user/age,List(ValidationError(validation.required,WrappedArray())))))
```

The `Failure` tells us that it could not find `/user/age` in that `JsValue`.

That example is nice, but we'd certainly prefer ton extract `age` as an `Int` rather than a `JsValue`.
All we have to do is to change the output type in our `Rule` definition:

```scala
val age = (Path \ "user" \ "age").read[JsValue, Int]
```

And apply it:

```scala
scala> age.validate(js)
res1: play.api.data.mapping.VA[JsValue, Int] = Success(25)
```

If we try to parse something that's not an `Int`, we get a `Failure` with the appropriate Path and error:

```scala
scala> (Path \ "user" \ "name").read[JsValue, Int].validate(js)
res8: VA[JsValue,Int] = Failure(List((/user/name,List(ValidationError(validation.type-mismatch,WrappedArray(Int))))))
```

So scala *automagically* figures out how transform a `JsValue` into an `Int`. How does this happens ?

It's fairly simple, the definition of `read` looks like this:

```scala
def read[I, O](implicit r: Path => Rule[I, O]): Rule[I, O]
```

So when use `(Path \ "user" \ "age").read[JsValue, Int]`, the compiler looks for an `implicit Path => Rule[JsValue, Int]`, which happens to exist in `play.api.data.mapping.json.Rules`.


### Validation

So var we've managed to lookup for a `JsValue` and transform that `JsValue` into an `Int`. Problem is: not every `Int` is a valid age. An age should always be a positive `Int`.

```scala
val js = Json.parse("""{
  "user": {
    "age" : -33
  }
}""")

val age = (Path \ "user" \ "age").read[JsValue, Int]
```

Our current implementation of `age` is rather unsatisfying...

```scala
scala> age.validate(js)
res1: VA[JsValue, Int] = Success(-33)
```

We can fix that very simply using `from`, and a built-in `Rule`:

```scala
scala> val age = (Path \ "user" \ "age").from[JsValue](min(0))
age: Rule[JsValue, Int] = play.api.data.mapping.Rule$$anon$2@6ae9b01
```

Let's try that again:

```scala
scala> age.validate(js)
res1: VA[JsValue, Int] = Failure(List((/user/age,List(ValidationError(validation.min,WrappedArray(0))))))
```
That's better, but still not perfect: 8765 is considered valid:

```scala
val js = Json.parse("""{ "user": { "age" : 8765 } }""")
age.validate(js) // Success(8765)
```

Let's fix our `age` `Rule`:

```scala
val age = (Path \ "user" \ "age").from[JsValue](min(0) |+| max(130))
```

and test it:

```scala
val js = Json.parse("""{ "user": { "age" : 8765 } }""")
age.validate(js) // Failure(ArrayBuffer((/user/age,List(ValidationError(validation.max,WrappedArray(130))))))
```

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

## Combining Rules

## Real world example

## Cookbook

### Dependant values
### Recursive types
### Read keys
