/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
import play.data.validation.ValidationError;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;
import javaguide.forms.u1.User;

import java.time.LocalTime;
import java.util.*;

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
        //#create
        Form<User> userForm = formFactory.form(User.class);
        //#create

        //#bind
        Map<String,String> anyData = new HashMap<>();
        anyData.put("email", "bob@gmail.com");
        anyData.put("password", "secret");

        User user = userForm.bind(anyData).get();
        //#bind

        assertThat(user.getEmail(), equalTo("bob@gmail.com"));
        assertThat(user.getPassword(), equalTo("secret"));
    }

    @Test
    public void bindFromRequest() {
        Result result = call(new Controller1(instanceOf(JavaHandlerComponents.class)),
                fakeRequest("POST", "/").bodyForm(ImmutableMap.of("email", "e", "password", "p")), mat);
        assertThat(contentAsString(result), equalTo("e"));
    }

    public class Controller1 extends MockJavaAction {

        Controller1(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        public Result index() {
            Form<User> userForm = formFactory().form(User.class);
            //#bind-from-request
            User user = userForm.bindFromRequest().get();
            //#bind-from-request

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
        Result result = call(new U3UserController(instanceOf(JavaHandlerComponents.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of("email", "e", "password", "p")), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("Invalid email or password"));
    }

    public class U3UserController extends MockJavaAction {

        U3UserController(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        public Result index() {
            Form<javaguide.forms.u3.User> userForm = formFactory().form(javaguide.forms.u3.User.class).bindFromRequest();

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
        Result result = call(new ListValidationController(instanceOf(JavaHandlerComponents.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of("email", "e")), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This e-mail is already registered."));
    }

    public static class UserForm {
        public static class User {
            public static String byEmail(String email) {
                return email;
            }
        }

        private String email;

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        //#list-validate
        public List<ValidationError> validate() {
            List<ValidationError> errors = new ArrayList<ValidationError>();
            if (User.byEmail(email) != null) {
                errors.add(new ValidationError("email", "This e-mail is already registered."));
            }
            return errors.isEmpty() ? null : errors;
        }
        //#list-validate
    }

    public class ListValidationController extends MockJavaAction {

        ListValidationController(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        public Result index() {
            Form<UserForm> userForm = formFactory().form(UserForm.class).bindFromRequest();

            if (userForm.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(userForm));
            } else {
                UserForm user = userForm.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void handleErrors() {
        Result result = call(new Controller2(instanceOf(JavaHandlerComponents.class)), fakeRequest("POST", "/")
            .bodyForm(ImmutableMap.of("email", "e")), mat);
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
            //#handle-errors
            if (userForm.hasErrors()) {
                return badRequest(views.html.form.render(userForm));
            } else {
                User user = userForm.get();
                return ok("Got user " + user);
            }
            //#handle-errors
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
        //#fill
        userForm = userForm.fill(new User("bob@gmail.com", "secret"));
        //#fill
        assertThat(userForm.field("email").getValue().get(), equalTo("bob@gmail.com"));
        assertThat(userForm.field("password").getValue().get(), equalTo("secret"));
    }

    @Test
    public void dynamicForm() {
        Result result = call(new Controller3(instanceOf(JavaHandlerComponents.class)),
                fakeRequest("POST", "/").bodyForm(ImmutableMap.of("firstname", "a", "lastname", "b")), mat);
        assertThat(contentAsString(result), equalTo("Hello a b"));
    }

    public class Controller3 extends MockJavaAction {
        FormFactory formFactory = formFactory();

        Controller3(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        //#dynamic
        public Result hello() {
            DynamicForm requestData = formFactory.form().bindFromRequest();
            String firstname = requestData.get("firstname");
            String lastname = requestData.get("lastname");
            return ok("Hello " + firstname + " " + lastname);
        }
        //#dynamic
    }

    @Test
    public void registerFormatter() {
        Application application = new GuiceApplicationBuilder()
            .overrides(bind(Formatters.class).toProvider(FormattersProvider.class))
            .build();

        Form<WithLocalTime> form = application.injector().instanceOf(FormFactory.class).form(WithLocalTime.class);
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

}
