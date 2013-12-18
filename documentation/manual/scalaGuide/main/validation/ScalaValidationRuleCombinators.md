# Combining Rules

## Introduction

We've already explained what a `Rule` is in [[the previous chapter | ScalaValidationRule]].
Those examples were only covering simple rules. However most of the time, rules are used to validate and transform complex hierarchical objects, like [[Json|ScalaValidationJson]], or [[Forms|ScalaValidationForm]].

The validation API allows complex object rules creation by combining simple rules together. This chapter explains how to create complex rules.

> Despite examples below are validating Json objects, the API is not dedicated only to Json and can be used on any type.
> Please refer to [[Validating Json | ScalaValidationJson]], [[Validating Forms|ScalaValidationForm]], and [[Supporting new types|ScalaValidationExtensions]] for more information.

## Path

The validation API defines a class named `Path`. A `Path` represents the location of a data among a complex object.
Unlike `JsPath` it is not related to any specific type. It's just a location in some data.
Most of the time, a `Path` is our entry point into the Validation API.

A `Path` is declared using this syntax:

@[combinators-path](code/ScalaValidationRuleCombinators.scala)

`Path` here is the empty `Path` object. One may call it the root path.

A path can also reference indexed data, such as a `Seq`

@[combinators-path-index](code/ScalaValidationRuleCombinators.scala)

### Extracting data using `Path`

Consider the following json:

@[combinators-js](code/ScalaValidationRuleCombinators.scala)

The first step before validating anything is to be able to access a fragment of the complex object.

Assuming you'd like to validate that `friend` exists and is valid in this json, you first need to access the object located at `user.friend` (Javascript notation).

#### The `read` method

We start by creating a `Path` representing the location of the data we're interested in:

@[combinators-path-location](code/ScalaValidationRuleCombinators.scala)

`Path` has a `read[I, O]` method, where `I` represents the input we're trying to parse, and `O` the output type. For example, `(Path \ "foo").read[JsValue, Int]`, will try to read a value located at path `/foo` in a `JsValue` as an `Int`.

But let's try something much easier for now:

@[rule-extract](code/ScalaValidationRuleCombinators.scala)

`location.read[JsValue, JsValue]` means we're trying to lookup at `location` in a `JsValue`, and we expect to find a `JsValue` there.
In fact we're defining a `Rule` that is picking a subtree in a `JsValue`.

If you try to run that code, the compiler gives you the following error:

```
error: No implicit view available from play.api.data.mapping.Path => play.api.data.mapping.Rule[play.api.libs.json.JsValue,play.api.libs.json.JsValue].
       val findFriend: Rule[JsValue, JsValue] = location.read[JsValue, JsValue]
                                                             ^
```

The Scala compiler is complaining about not finding an implicit function of type `Path => Rule[JsValue, JsValue]`. Indeed, unlike the Json API, you have to provide a method to **lookup** into the data you expect to validate.

Fortunately, such method already exists. All you have to do is to import it:

@[rule-extract-import](code/ScalaValidationRuleCombinators.scala)

> By convention, all useful validation methods for a given type are to be found in an object called `Rules`. That object contains a bunch of implicits defining how to lookup in the data, and how to coerce some of the possible values of those data into Scala types.

With those implicits in scope, we can finally create our `Rule`:

@[rule-extract](code/ScalaValidationRuleCombinators.scala)

Alright, so far we've defined a `Rule` looking for some data of type `JsValue`, located at `/user/friend` in an object of type `JsValue`.

Now we need to apply this `Rule` to our data:

@[rule-extract-test](code/ScalaValidationRuleCombinators.scala)

If we can't find anything, applying a `Rule` leads to a `Failure`:

@[rule-extract-fail](code/ScalaValidationRuleCombinators.scala)

### Type coercion

We now are capable of extracting data at a given `Path`. Let's do it again on a different sub-tree:

@[rule-extract-age](code/ScalaValidationRuleCombinators.scala)

Let's apply this new `Rule`:

@[rule-extract-age-test](code/ScalaValidationRuleCombinators.scala)

Again, if the json is invalid:

@[rule-extract-age-fail](code/ScalaValidationRuleCombinators.scala)

The `Failure` informs us that it could not find `/user/age` in that `JsValue`.

That example is nice, but we'd certainly prefer to extract `age` as an `Int` rather than a `JsValue`.
All we have to do is to change the output type in our `Rule` definition:

@[rule-extract-ageInt](code/ScalaValidationRuleCombinators.scala)

And apply it:

@[rule-extract-ageInt-test](code/ScalaValidationRuleCombinators.scala)

If we try to parse something that is not an `Int`, we get a `Failure` with the appropriate Path and error:

@[rule-extract-ageInt-fail](code/ScalaValidationRuleCombinators.scala)

So scala *automagically* figures out how to transform a `JsValue` into an `Int`. How does this happens ?

It's fairly simple. The definition of `read` looks like this:

```scala
def read[I, O](implicit r: Path => Rule[I, O]): Rule[I, O]
```

So when use `(Path \ "user" \ "age").read[JsValue, Int]`, the compiler looks for an `implicit Path => Rule[JsValue, Int]`, which happens to exist in `play.api.data.mapping.json.Rules`.


### Validation

So far we've managed to lookup for a `JsValue` and transform that `JsValue` into an `Int`. Problem is: not every `Int` is a valid age. An age should always be a positive `Int`.

@[rule-validate-js](code/ScalaValidationRuleCombinators.scala)

Our current implementation of `age` is rather unsatisfying...

@[rule-validate-testNeg](code/ScalaValidationRuleCombinators.scala)

We can fix that very simply using `from`, and a built-in `Rule`:

@[rule-validate-pos](code/ScalaValidationRuleCombinators.scala)

Let's try that again:

@[rule-validate-pos-test](code/ScalaValidationRuleCombinators.scala)

That's better, but still not perfect: 8765 is considered valid:

@[rule-validate-pos-big](code/ScalaValidationRuleCombinators.scala)

Let's fix our `age` `Rule`:

@[rule-validate-proper](code/ScalaValidationRuleCombinators.scala)

and test it:

@[rule-validate-proper-test](code/ScalaValidationRuleCombinators.scala)

### Full example

@[rule-validate-full](code/ScalaValidationRuleCombinators.scala)

## Combining Rules

So far we've validated only fragments of our json object.
Now we'd like to validate the entire object, and turn it into a instance of the `User` class defined below:

@[rule-combine-user](code/ScalaValidationRuleCombinators.scala)

We need to create a `Rule[JsValue, User]`. Creating this Rule is simply a matter of combining together the rules parsing each field of the json.

@[rule-combine-rule](code/ScalaValidationRuleCombinators.scala)

> **Important:** Note that we're importing `Rules._` **inside** the `From[I]{...}` block.
It is recommended to always follow this pattern, as it nicely scopes the implicits, avoiding conflicts and accidental shadowing.

`From[JsValue]` defines the `I` type of the rules we're combining. We could have written:

```scala
 (Path \ "name").read[JsValue, String] and
 (Path \ "age").read[JsValue, Int] and
 //...
```

but repeating `JsValue` all over the place is just not very DRY.

> **Next:** - [[Serialization with Write | ScalaValidationWrite]]
> **For more examples and snippets:** - [[Cookbook | ScalaValidationCookbook]]
