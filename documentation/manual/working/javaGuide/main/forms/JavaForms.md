<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling form submission

Before you start with Play forms, read the documentation on the [[Play enhancer|PlayEnhancer]]. The Play enhancer generates accessors for fields in Java classes for you, so that you don't have to generate them yourself. You may decide to use this as a convenience. All the examples below show manually writing accessors for your classes.

## Enabling/Disabling the forms module

By default, Play includes the Java forms module (`play-java-forms`) when enabling the `PlayJava` sbt plugin, so there is nothing to enable if you already have `enablePlugins(PlayJava)` on your project.

The forms module is also available in `PlayImport` as `javaForms`, which can be used with `libraryDependencies += javaForms` in your `build.sbt`.

> **Note:** If you are not using forms, you can remove the forms dependency by using the `PlayMinimalJava` SBT plugin instead of `PlayJava`. This also allows you to remove several transitive dependencies only used by the forms module, including several Spring modules and the Hibernate validator.

## Defining a form

The `play.data` package contains several helpers to handle HTTP form data submission and validation. The easiest way to handle a form submission is to define a `play.data.Form` that wraps an existing class:

@[user](code/javaguide/forms/u1/User.java)

To wrap a class you have to inject a [`play.data.FormFactory`](api/java/play/data/FormFactory.html) into your Controller which then allows you to create the form:

@[create](code/javaguide/forms/JavaForms.java)

> **Note:** The underlying binding is done using [Spring data binder](https://docs.spring.io/spring/docs/4.2.4.RELEASE/spring-framework-reference/html/validation.html).

This form can generate a `User` result value from `HashMap<String,String>` data:

@[bind](code/javaguide/forms/JavaForms.java)

If you have a request available in the scope, you can bind directly from the request content:

@[bind-from-request](code/javaguide/forms/JavaForms.java)

## Defining constraints

You can define additional constraints that will be checked during the binding phase using [`JSR-303` (Bean Validation 1.0)](http://beanvalidation.org/1.0/spec/) annotations:

@[user](code/javaguide/forms/u2/User.java)

> **Tip:** The `play.data.validation.Constraints` class contains several built-in validation annotations.

In the [Advanced validation](#advanced-validation) section further below you will learn how to handle concerns like cross field validation, partial form validation or how to make use of injected components (e.g. to access a database) during validation.

## Handling binding failure

Of course if you can define constraints, then you need to be able to handle the binding errors.

@[handle-errors](code/javaguide/forms/JavaForms.java)

Typically, as shown above, the form simply gets passed to a template.  Global errors can be rendered in the following way:

@[global-errors](code/javaguide/forms/view.scala.html)

Errors for a particular field can be rendered in the following manner with [`error.format`](api/scala/play/api/data/FormError.html):

@[field-errors](code/javaguide/forms/view.scala.html)

Note that `error.format` takes `messages()` as an argument -- this is an [`play.18n.Messages`](api/java/play/i18n/Messages.html) instance defined in [[JavaI18N]].

## Filling a form with initial default values

Sometimes you’ll want to fill a form with existing values, typically for editing:

@[fill](code/javaguide/forms/JavaForms.java)

> **Tip:** `Form` objects are immutable - calls to methods like `bind()` and `fill()` will return a new object filled with the new data.

## Handling a form that is not related to a Model

You can use a `DynamicForm` if you need to retrieve data from an html form that is not related to a `Model`:

@[dynamic](code/javaguide/forms/JavaForms.java)

## Register a custom DataBinder

In case you want to define a mapping from a custom object to a form field string and vice versa you need to register a new Formatter for this object.
You can achieve this by registering a provider for `Formatters` which will do the proper initialization.
For an object like JavaTime's `LocalTime` it could look like this:

@[register-formatter](code/javaguide/forms/FormattersProvider.java)

After defining the provider you have to bind it:

@[register-formatter](code/javaguide/forms/FormattersModule.java)

Finally you have to disable Play's default `FormattersModule` and instead enable your module in `application.conf`:

    play.modules.enabled += "com.example.FormattersModule"
    play.modules.disabled += "play.data.format.FormattersModule"

When the binding fails an array of errors keys is created, the first one defined in the messages file will be used. This array will generally contain:

    ["error.invalid.<fieldName>", "error.invalid.<type>", "error.invalid"]

The errors keys are created by [Spring DefaultMessageCodesResolver](https://docs.spring.io/spring/docs/4.2.4.RELEASE/javadoc-api/org/springframework/validation/DefaultMessageCodesResolver.html), the root "typeMismatch" is replaced by "error.invalid".

## Advanced validation

Play's built-in validation module is using [Hibernate Validator](http://hibernate.org/validator/) under the hood. This means we can take advantage of features defined in the [`JSR-303` (Bean Validation 1.0)](http://beanvalidation.org/1.0/spec/) and [`JSR-349` (Bean Validation 1.1)](http://beanvalidation.org/1.1/spec/1.1.0.cr3/). The Hibernate Validator documentation can be found [here](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/).

### Cross field validation

To validate the state of an entire object we can make use of [class-level constraints](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/#section-class-level-constraints).
To free you from the burden of implementing your own class-level constraint(s), Play out-of-the-box already provides a generic implementation of such constraint which should cover at least the most common use cases.

Now let's see how this works: To define an ad-hoc validation, all you need to do is annotate your form class with Play's provided class-level constraint (`@Validate`) and implement the corresponding interface (in this case `Validatable<String>`) - which forces you to override a `validate` method:

@[user](code/javaguide/forms/u3/User.java)

The message returned in the above example will become a global error. Errors are defined as [`play.data.validation.ValidationError`](api/java/play/data/validation/ValidationError.html).
Also be aware that in this example the `validate` method and the `@Constraints.Required` constraint will be called simultaneously - so the `validate` method will be called no matter if `@Constraints.Required` was successful or not (and vice versa). You will learn how to introduce an order later on.

As you can see the `Validatable<T>` interface takes a type parameter which determines the return type of the `validate()` method.
So depending if you want to be able to add a single global error, one error (which could be global as well) or multiple (maybe global) errors to a form via `validate()`, you have to use either a `String`, a `ValidationError` or a `List<ValidationError>` as type argument. Any other return types of the validate method will be ignored by Play.

If validation passes inside a `validate()` method you must return `null` or an empty `List`. Returning any other non-`null` value (including empty string) is treated as failed validation.

Returning a `ValidationError` object may be useful when you have additional validations for a specific field:

@[object-validate](code/javaguide/forms/JavaForms.java)

You can add multiple validation errors by returning `List<ValidationError>`. This can be used to add validation errors for a specific field, global errors or even a mix of these options:

@[list-validate](code/javaguide/forms/JavaForms.java)

As you can see, when using an empty string as the key of a `ValidationError` it becomes a global error.

One more thing: Instead of writing out error messages you can use message keys defined in `conf/messages` and pass arguments to them. When displaying the validation errors in a template the message keys and it's arguments will be automatically resolved by Play:

@[validation-error-examples](code/javaguide/forms/JavaForms.java)

### Partial form validation via groups

When a user submits a form there can be use cases where you don't want to validate all constraints at once but just some of them. For example think about a UI wizard where in each step only a specified subset of constraints should get validated.

Or think about the sign-up and the login process of a web application. Usually for both processes you want the user to enter an email address and a password. So these processes would require almost the same forms, except for the sign-up process the user also has to enter a password confirmation. To make things more interesting let's assume a user can also change his user data on a settings page when he is logged in already - which would need a third form.

Using three different forms for such a case isn't really a good idea because you would use the same constraint annotations for most of the form fields anyway. What if you have defined a max-length constraint of 255 for a `name` field and then want to change it to a limit of just 100? You would have to change this for each form. As you can imagine this would be error prone in case you forget to update one of the forms.

Luckily we can simply [group constraints](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/#chapter-groups):

@[user](code/javaguide/forms/groups/PartialUserForm.java)

The `SignUpCheck` and `LoginCheck` group are defined as two interfaces:

@[check](code/javaguide/forms/groups/SignUpCheck.java)

@[check](code/javaguide/forms/groups/LoginCheck.java)

For the sign-up process we simply pass the `SignUpCheck` group to the `form(...)` method:

@[partial-validate-signup](code/javaguide/forms/JavaForms.java)

In this case the email address is required and has to be a valid email address, both the password and the password confirmation are required and the two passwords have to be equal (because of the `@Validate` annotation which calls the `validate` method). But we don't care about the first name and last name - they can be empty or we could even exclude these input fields in the sign up page.

For the login process we just pass the `LoginCheck` group instead:

@[partial-validate-login](code/javaguide/forms/JavaForms.java)

Now we only require the email address and the password to be entered - nothing more. We don't even care about if the email is valid. You probably wouldn't display any of the other form fields to the user because we don't validate them anyway.

Imagine we also have a page where the user can change the user data (but not the password):

@[partial-validate-default](code/javaguide/forms/JavaForms.java)

Which is exactly the same as:

@[partial-validate-nogroup](code/javaguide/forms/JavaForms.java)

In this case following constraints will be validated: The email address is required and has to be valid plus the first name and last name are required as well - that is because if a constraint annotation doesn't *explicitly* define a `group` then the `Default` group is used.
Be aware we don't check any of the password constraints: Because they *explicitly* define a `group` attribute but don't include the `Default` group they won't be taken into account here.

As you can see in the last example, when **only** passing the group `javax.validation.groups.Default` you can omit it - because it's the default anyway.
But as soon you pass any other group(s) you would also have to pass the `Default` group *explicitly* if you want any of it's fields taken into account during the validation process.

> **Tip:** You can pass as many groups as you like to the `form(...)` method (not just one). Just to be clear: These groups will then be validated all at once - *not* one after the other.

For advanced usage a group of constraints can include another group. You can do that using [group inheritance](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/#section-group-inheritance).

### Defining the order of constraint groups

You can validate groups [in sequences](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/#section-defining-group-sequences). This means groups will be validated one after another - but the next group will only be validated if the previous group was validated successfully before. (However right now it's not possible to determine the order of how constraints will be validated *within* a group itself - [this is part](https://hibernate.atlassian.net/browse/BVAL-248) of `JSR-380` - Bean Validation 2.0 - which is still [in draft](http://beanvalidation.org/proposals/BVAL-248/))

Based on the example above let's define a group sequence:

@[ordered-checks](code/javaguide/forms/groupsequence/OrderedChecks.java)

Now we can use it:

@[ordered-group-sequence-validate](code/javaguide/forms/JavaForms.java)

Using this group sequence will first validate all fields belonging to the `Default` group (which again also includes fields that haven't defined a group at all). Only when all the fields belonging to the `Default` group pass validation successfully, the fields belonging to the `SignUpCheck` will be validated and so on.

Using a group sequence is especially a good practice when you have a `validate` method which queries a database or performs any other blocking action: It's not really useful to execute the method at all if the validation fails at it's basic level (email is not valid, number is a string, etc). In such a case you probably want the `validate` be called only after checking all other annotation-based constraints before and only if they pass. A user, for example, who signs up should enter a valid email address and *only* if it is valid a database lookup for the email address should be done *afterwards*.

### Custom class-level constraints with DI support

Sometimes you need more sophisticated validation processes. E.g. when a user signs up you want to check if his email address already exists in the database and if so validation should fail.

Because constraints support both [[runtime Dependency Injection|JavaDependencyInjection]] and [[|JavaCompileTimeDependencyInjection]], we can easily create our own custom (class-level) constraint which gets a `Database` object injected - which we can use later in the validation process. Of course you can also inject other components like `MessagesApi`, `JPAApi`, etc.

> **Note:** You only need to create one class-level constraint for each cross concern. For example, the constraint we will create in this section is reusable and can be used for all validation processes where you need to access the database. The reason why Play doesn't provide any generic class-level constraints with dependency injected components is because Play doesn't know which components you might have enabled in your project.

First let's set up the interface with the `validate` method we will implement in our form later. You can see the method gets passed a `Database` object (Checkout the [[database docs|JavaDatabase]]):

@[interface](code/javaguide/forms/customconstraint/ValidatableWithDB.java)

We also need the class-level annotation we put on our form class:

@[annotation](code/javaguide/forms/customconstraint/ValidateWithDB.java)

Finally this is how our constraint implementation looks like:

@[constraint](code/javaguide/forms/customconstraint/ValidateWithDBValidator.java)

As you can see we inject the `Database` object into the constraint's constructor and use it later when calling `validate`. When using runtime Dependency Injection, Guice will automatically inject the `Database` object, but for compile-time Dependency Injection you need to do that by yourself:

@[constraint-compile-timed-di](code/javaguide/forms/customconstraint/ValidateWithDBComponents.java)

> **Note**: you don't need to create the `database` instance by yourself, it is already defined in the implemented interfaces.

This way, your validator will be available when necessary.

When writing your own class-level constraints you can pass following objects to the `reportValidationStatus` method: A `ValidationError`, a `List<ValidationError>` or a `String` (handled as global error). Any other objects will be ignored by Play.

Finally we can use our custom class-level constraint to validate a form:

@[user](code/javaguide/forms/customconstraint/DBAccessForm.java)

> **Tip:** You might have recognized that you could even implement multiple interfaces and therefore add multiple class-level constraint annotations on your form class. Via validation groups you could then just call the desired validate method(s) (or even multiple at once during one validation process).
