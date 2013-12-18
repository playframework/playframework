# Cookbook

> All the examples below are validating Json objects. The API is not dedicated only to Json, it can be used on any type. Please refer to [[Validating Json | ScalaValidationJson]], [[Validating Forms|ScalaValidationForm]], and [[Supporting new types|ScalaValidationExtensions]] for more information.

## `Rule`

### Typical case class validation

@[validate-case-class](code/ScalaValidationCookbook.scala)

### Dependent values

A common example of this use case is the validation of `password` and `password confirmation` fields in a signup form.

1. First, you need to validate that each field is valid independently
2. Then, given the two values, you need to validate that they are equals.

@[validate-dependent](code/ScalaValidationCookbook.scala)

Splitting up the code:

@[validate-dependent1](code/ScalaValidationCookbook.scala)

This code creates a `Rule[JsValue, (String, String)]` each of of the String must be non-empty

@[validate-dependent2](code/ScalaValidationCookbook.scala)

We then create a `Rule[(String, String), String]` validating that given a `(String, String)`, both strings are equals. Those rules are then composed together.

In case of `Failure`, we want to control the field holding the errors. We change the `Path` of errors using `repath`:

@[validate-dependent3](code/ScalaValidationCookbook.scala)

Let's test it:

@[validate-dependent-tests](code/ScalaValidationCookbook.scala)

### Recursive types

When validating recursive types:

- Use the `lazy` keyword to allow forward reference.
- As with any recursive definition, the type of the `Rule` **must** be explicitly given.

@[validate-recursive-data](code/ScalaValidationCookbook.scala)

@[validate-recursive](code/ScalaValidationCookbook.scala)

or using macros:

@[validate-recursive-macro](code/ScalaValidationCookbook.scala)

### Read keys

@[validate-read-keys](code/ScalaValidationCookbook.scala)

### Validate subclasses (and parse the concrete class)

Consider the following class definitions:

@[validate-recursive-def](code/ScalaValidationCookbook.scala)

#### Trying all the possible rules implementations

@[validate-recursive-testall](code/ScalaValidationCookbook.scala)

#### Using class discovery based on field discrimination

@[validate-recursive-field](code/ScalaValidationCookbook.scala)

## `Write`

### typical case class `Write`

@[write-case](code/ScalaValidationCookbook.scala)

### Adding static values to a `Write`

@[write-static](code/ScalaValidationCookbook.scala)





