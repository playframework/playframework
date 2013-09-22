# Using Custom Validations

The [validation package](api/scala/index.html#play.api.data.validation.package) allows you to create ad-hoc constraints using the `verifying` method.  However, Play gives you the option of creating your own custom constraints, using the [`Constraint`](api/scala/index.html#play.api.data.validation.Constraint) case class.

Here, we'll implement a simple password strength constraint that uses regular expressions to check the password is not all letters or all numbers.  A [`Constraint`](api/scala/index.html#play.api.data.validation.Constraint) takes a function which returns a [`ValidationResult`](api/scala/index.html#play.api.data.validation.ValidationResult), and we use that function to return the results of the password check:

@[passwordcheck-constraint](code/CustomValidations.scala)

> **Note:** This is an intentionally trivial example.  Please consider using the [OWASP guide](https://www.owasp.org/index.php/Authentication_Cheat_Sheet#Implement_Proper_Password_Strength_Controls) for proper password security.

We can then use this constraint together with [`Constraints.min`](api/scala/index.html#play.api.data.validation.Constraints) to add additional checks on the password.

@[passwordcheck-mapping](code/CustomValidations.scala)

> **Next:** [[Custom Field Constructors|ScalaCustomFieldConstructors]]
