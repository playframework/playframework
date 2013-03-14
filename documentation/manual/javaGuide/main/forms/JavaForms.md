# Handling form submission

## Defining a form

The `play.data` package contains several helpers to handle HTTP form data submission and validation. The easiest way to handle a form submission is to define a `play.data.Form` that wraps an existing class:

```java
public class User {
    public String email;
    public String password;
}
```

```java
Form<User> userForm = form(User.class);
```

> **Note:** The underlying binding is done using [[Spring data binder| http://static.springsource.org/spring/docs/3.0.7.RELEASE/reference/validation.html]].

This form can generate a `User` result value from `HashMap<String,String>` data:

```java
Map<String,String> anyData = new HashMap();
anyData.put("email", "bob@gmail.com");
anyData.put("password", "secret");

User user = userForm.bind(anyData).get();
```

If you have a request available in the scope, you can bind directly from the request content:

```java
User user = userForm.bindFromRequest().get();
```

## Defining constraints

You can define additional constraints that will be checked during the binding phase using JSR-303 (Bean Validation) annotations:

```java
public class User {
    
    @Required
    public String email;
    public String password;
}
```

> **Tip:** The `play.data.validation.Constraints` class contains several built-in validation annotations.

You can also define an ad-hoc validation by adding a `validate` method to your top object:

```java
public class User {
    
    @Required
    public String email;
    public String password;
    
    public String validate() {
        if(authenticate(email,password) == null) {
            return "Invalid email or password";
        }
        return null;
    }
}
```

In this example the message goes to `globalError` and can be retrieved in your templates in the following way:

```html
@if(form.hasGlobalErrors) {
    <p class="error">
        @form.globalError.message
    </p>
}
```

Since 2.0.2 the `validate`-method can return the following types: `String`, `List<ValidationError>` or `Map<String,List<ValidationError>>`

`validate` method is called after checking annotation-based constraints and only if they pass.
If validation passes you must return `null` . Returning any not-`null` value (empty string or empty list) is treated as failed validation.

Usage `List<ValidationError>` may be useful when you have additional validations for fields. For example:

```java
public List<ValidationError> validate() {
    List<ValidationError> errors = new ArrayList<>();
    if (User.byEmail(email) != null) {
        errors.add(new ValidationError("email", "Such e-mail is already registered."));
    }
    return errors.isEmpty() ? null : errors;
}
```

You can retrieve errors for `email` field in the following manner:

```html
@for(error <- someForm("email").errors) {
    <p>@error.message</p>
}
```

Using `Map<String,List<ValidationError>>` is similar to `List<ValidationError>` where map's keys are error codes similar to `email` in the example above.

## Handling binding failure

Of course if you can define constraints, then you need to be able to handle the binding errors.

```java
if(userForm.hasErrors()) {
    return badRequest(form.render(userForm));
} else {
    User user = userForm.get();
    return ok("Got user " + user);
}
```

## Filling a form with initial default values

Sometimes you’ll want to fill a form with existing values, typically for editing:

```java
userForm = userForm.fill(new User("bob@gmail.com", "secret"))
```

> **Tip:** `Form` objects are immutable - calls to methods like `bind()` and `fill()` will return a new object filled with the new data.

## Handling a form that is not related to a Model

You can use a `DynamicForm` if you need to retrieve data from an html form that is not related to a `Model` :

```java
public static Result hello(){
    DynamicForm requestData = form().bindFromRequest();
    String firstname = requestData.get("firstname");
    String lastname = requestData.get("lastname");
    return ok("Hello " + firstname + " " + lastname);
}
```

## Register a custom DataBinder

In case you want to define a mapping from a custom object to a form field string and vice versa you need to register a new Formatter for this object.
For an object like JodaTime's `LocalTime` it could look like this:

```java
Formatters.register(LocalTime.class, new SimpleFormatter<LocalTime>() {

    private Pattern timePattern = Pattern.compile(
        "([012]?\\\\d)(?:[\\\\s:\\\\._\\\\-]+([0-5]\\\\d))?"
    ); 
    
    @Override
    public LocalTime parse(String input, Locale l) throws ParseException {
        Matcher m = timePattern.matcher(input);
        if (!m.find()) throw new ParseException("No valid Input",0);
        int hour = Integer.valueOf(m.group(1));
        int min = m.group(2) == null ? 0 : Integer.valueOf(m.group(2));
        return new LocalTime(hour, min);
    }
    
    @Override
    public String print(LocalTime localTime, Locale l) {
        return localTime.toString("HH:mm");
    }
  
});
```

> **Next:** [[Using the form template helpers | JavaFormHelpers]]
