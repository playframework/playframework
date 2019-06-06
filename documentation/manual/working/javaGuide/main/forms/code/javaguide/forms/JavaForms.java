/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.data.format.Formatters;
import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;
import play.data.validation.ValidationError;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;
import javaguide.forms.groups.LoginCheck;
import javaguide.forms.groups.PartialUserForm;
import javaguide.forms.groups.SignUpCheck;
import javaguide.forms.groupsequence.OrderedChecks;
import javaguide.forms.u1.User;

import java.time.LocalTime;
import java.util.*;

import javax.validation.groups.Default;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static play.inject.Bindings.bind;

public class JavaForms extends WithApplication {

  private FormFactory formFactory() {
    return app.injector().instanceOf(FormFactory.class);
  }

  @Test
  public void usingForm() {
    FormFactory formFactory = formFactory();

    final // sneaky final
    // #create
    Form<User> userForm = formFactory.form(User.class);
    // #create

    // #bind
    Map<String, String> anyData = new HashMap<>();
    anyData.put("email", "bob@gmail.com");
    anyData.put("password", "secret");

    User user = userForm.bind(anyData).get();
    // #bind

    assertThat(user.getEmail(), equalTo("bob@gmail.com"));
    assertThat(user.getPassword(), equalTo("secret"));
  }

  @Test
  public void bindFromRequest() {
    Result result =
        call(
            new Controller1(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e", "password", "p")),
            mat);
    assertThat(contentAsString(result), equalTo("e"));
  }

  public class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      Form<User> userForm = formFactory().form(User.class);
      // #bind-from-request
      User user = userForm.bindFromRequest().get();
      // #bind-from-request

      return ok(user.getEmail());
    }
  }

  @Test
  public void constraints() {
    Form<javaguide.forms.u2.User> userForm = formFactory().form(javaguide.forms.u2.User.class);
    assertThat(userForm.bind(ImmutableMap.of("password", "p")).hasErrors(), equalTo(true));
  }

  @Test
  public void adhocValidation() {
    Result result =
        call(
            new U3UserController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e", "password", "p")),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("Invalid email or password"));
  }

  public class U3UserController extends MockJavaAction {

    U3UserController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      Form<javaguide.forms.u3.User> userForm =
          formFactory().form(javaguide.forms.u3.User.class).bindFromRequest();

      if (userForm.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(userForm));
      } else {
        javaguide.forms.u3.User user = userForm.get();
        return ok("Got user " + user);
      }
    }
  }

  public static String authenticate(String email, String password) {
    return null;
  }

  @Test
  public void listValidation() {
    Result result =
        call(
            new ListValidationController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e")),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("Access denied"));
    assertThat(contentAsString(result), containsString("Form could not be submitted"));
  }

  // #list-validate
  // ###insert: import play.data.validation.Constraints.Validate;
  // ###insert: import play.data.validation.Constraints.Validatable;
  // ###insert: import play.data.validation.ValidationError;
  // ###insert: import java.util.List;
  // ###insert: import java.util.ArrayList;

  @Validate
  public static class SignUpForm implements Validatable<List<ValidationError>> {

    // fields, getters, setters, etc.

    // ###skip: 19
    private String email;
    protected String password;

    public void setEmail(String email) {
      this.email = email;
    }

    public String getEmail() {
      return email;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPassword() {
      return password;
    }

    @Override
    public List<ValidationError> validate() {
      final List<ValidationError> errors = new ArrayList<>();
      if (authenticate(email, password) == null) {
        // Add an error which will be displayed for the email field:
        errors.add(new ValidationError("email", "Access denied"));
        // Also add a global error:
        errors.add(new ValidationError("", "Form could not be submitted"));
      }
      return errors;
    }
  }
  // #list-validate

  public class ListValidationController extends MockJavaAction {

    ListValidationController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      Form<SignUpForm> userForm = formFactory().form(SignUpForm.class).bindFromRequest();

      if (userForm.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(userForm));
      } else {
        SignUpForm user = userForm.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void objectValidation() {
    Result result =
        call(
            new ObjectValidationController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e")),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("Invalid credentials"));
  }

  // #object-validate
  // ###insert: import play.data.validation.Constraints.Validate;
  // ###insert: import play.data.validation.Constraints.Validatable;
  // ###insert: import play.data.validation.ValidationError;

  @Validate
  public static class LoginForm implements Validatable<ValidationError> {

    // fields, getters, setters, etc.

    // ###skip: 19
    private String email;
    private String password;

    public void setEmail(String email) {
      this.email = email;
    }

    public String getEmail() {
      return email;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPassword() {
      return password;
    }

    @Override
    public ValidationError validate() {
      if (authenticate(email, password) == null) {
        // Error will be displayed for the email field:
        return new ValidationError("email", "Invalid credentials");
      }
      return null;
    }
  }
  // #object-validate

  public class ObjectValidationController extends MockJavaAction {

    ObjectValidationController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      Form<LoginForm> adminForm = formFactory().form(LoginForm.class).bindFromRequest();

      if (adminForm.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(adminForm));
      } else {
        LoginForm user = adminForm.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void handleErrors() {
    Result result =
        call(
            new Controller2(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e")),
            mat);
    assertThat(contentAsString(result), startsWith("Got user"));
  }

  public class Controller2 extends MockJavaAction {
    Pviews views = new Pviews();

    class Pviews {
      Phtml html = new Phtml();
    }

    class Phtml {
      Pform form = new Pform();
    }

    class Pform {
      String render(Form<?> form) {
        return "rendered";
      }
    }

    Controller2(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      Form<User> userForm = formFactory().form(User.class).bindFromRequest();
      // #handle-errors
      if (userForm.hasErrors()) {
        return badRequest(views.html.form.render(userForm));
      } else {
        User user = userForm.get();
        return ok("Got user " + user);
      }
      // #handle-errors
    }
  }

  @Test
  public void fillForm() {
    // User needs a constructor. Give it one.
    class User extends javaguide.forms.u1.User {
      User(String email, String password) {
        this.email = email;
        this.password = password;
      }
    }
    Form<javaguide.forms.u1.User> userForm = formFactory().form(javaguide.forms.u1.User.class);
    // #fill
    userForm = userForm.fill(new User("bob@gmail.com", "secret"));
    // #fill
    assertThat(userForm.field("email").getValue().get(), equalTo("bob@gmail.com"));
    assertThat(userForm.field("password").getValue().get(), equalTo("secret"));
  }

  @Test
  public void dynamicForm() {
    Result result =
        call(
            new Controller3(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of("firstname", "a", "lastname", "b")),
            mat);
    assertThat(contentAsString(result), equalTo("Hello a b"));
  }

  public class Controller3 extends MockJavaAction {
    FormFactory formFactory = formFactory();

    Controller3(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #dynamic
    public Result hello() {
      DynamicForm requestData = formFactory.form().bindFromRequest();
      String firstname = requestData.get("firstname");
      String lastname = requestData.get("lastname");
      return ok("Hello " + firstname + " " + lastname);
    }
    // #dynamic
  }

  @Test
  public void registerFormatter() {
    Application application =
        new GuiceApplicationBuilder()
            .overrides(bind(Formatters.class).toProvider(FormattersProvider.class))
            .build();

    Form<WithLocalTime> form =
        application.injector().instanceOf(FormFactory.class).form(WithLocalTime.class);
    WithLocalTime obj = form.bind(ImmutableMap.of("time", "23:45")).get();
    assertThat(obj.getTime(), equalTo(LocalTime.of(23, 45)));
    assertThat(form.fill(obj).field("time").getValue().get(), equalTo("23:45"));
  }

  public static class WithLocalTime {
    private LocalTime time;

    public LocalTime getTime() {
      return time;
    }

    public void setTime(LocalTime time) {
      this.time = time;
    }
  }

  public void validationErrorExamples() {
    final String arg1 = "";
    final String arg2 = "";
    final String email = "";

    // #validation-error-examples
    // Global error without internationalization:
    new ValidationError("", "Errors occurred. Please check your input!");
    // Global error; "validationFailed" should be defined in `conf/messages` - taking two arguments:
    new ValidationError("", "validationFailed", Arrays.asList(arg1, arg2));
    // Error for the email field; "emailUsedAlready" should be defined in `conf/messages` - taking
    // the email as argument:
    new ValidationError("email", "emailUsedAlready", Arrays.asList(email));
    // #validation-error-examples
  }

  @Test
  public void partialFormSignupValidation() {
    Result result =
        call(
            new PartialFormSignupController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of()),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("This field is required"));
  }

  public class PartialFormSignupController extends MockJavaAction {

    PartialFormSignupController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      // #partial-validate-signup
      Form<PartialUserForm> form =
          formFactory().form(PartialUserForm.class, SignUpCheck.class).bindFromRequest();
      // #partial-validate-signup

      if (form.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(form));
      } else {
        PartialUserForm user = form.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void partialFormLoginValidation() {
    Result result =
        call(
            new PartialFormLoginController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of()),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("This field is required"));
  }

  public class PartialFormLoginController extends MockJavaAction {

    PartialFormLoginController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      // #partial-validate-login
      Form<PartialUserForm> form =
          formFactory().form(PartialUserForm.class, LoginCheck.class).bindFromRequest();
      // #partial-validate-login

      if (form.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(form));
      } else {
        PartialUserForm user = form.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void partialFormDefaultValidation() {
    Result result =
        call(
            new PartialFormDefaultController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of()),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("This field is required"));
  }

  public class PartialFormDefaultController extends MockJavaAction {

    PartialFormDefaultController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      // #partial-validate-default
      Form<PartialUserForm> form =
          formFactory().form(PartialUserForm.class, Default.class).bindFromRequest();
      // #partial-validate-default

      if (form.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(form));
      } else {
        PartialUserForm user = form.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void partialFormNoGroupValidation() {
    Result result =
        call(
            new PartialFormNoGroupController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of()),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("This field is required"));
  }

  public class PartialFormNoGroupController extends MockJavaAction {

    PartialFormNoGroupController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      // #partial-validate-nogroup
      Form<PartialUserForm> form = formFactory().form(PartialUserForm.class).bindFromRequest();
      // #partial-validate-nogroup

      if (form.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(form));
      } else {
        PartialUserForm user = form.get();
        return ok("Got user " + user);
      }
    }
  }

  @Test
  public void OrderedGroupSequenceValidation() {
    Result result =
        call(
            new OrderedGroupSequenceController(instanceOf(JavaHandlerComponents.class)),
            fakeRequest("POST", "/").bodyForm(ImmutableMap.of()),
            mat);

    // Run it through the template
    assertThat(contentAsString(result), containsString("This field is required"));
  }

  public class OrderedGroupSequenceController extends MockJavaAction {

    OrderedGroupSequenceController(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    public Result index() {
      // #ordered-group-sequence-validate
      Form<PartialUserForm> form =
          formFactory().form(PartialUserForm.class, OrderedChecks.class).bindFromRequest();
      // #ordered-group-sequence-validate

      if (form.hasErrors()) {
        return badRequest(javaguide.forms.html.view.render(form));
      } else {
        PartialUserForm user = form.get();
        return ok("Got user " + user);
      }
    }
  }
}
