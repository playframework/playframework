/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms;

import com.google.common.collect.ImmutableMap;
import org.joda.time.LocalTime;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.Form;
import play.data.format.Formatters;
import play.data.format.Formatters.SimpleFormatter;
import play.data.validation.ValidationError;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.forms.u1.User;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaForms extends WithApplication {

    @Test
    public void usingForm() {
        final // sneaky final
        //#create
        Form<User> userForm = Form.form(User.class);
        //#create

        //#bind
        Map<String,String> anyData = new HashMap();
        anyData.put("email", "bob@gmail.com");
        anyData.put("password", "secret");

        User user = userForm.bind(anyData).get();
        //#bind

        assertThat(user.email, equalTo("bob@gmail.com"));
        assertThat(user.password, equalTo("secret"));
    }

    @Test
    public void bindFromRequest() {
        Result result = MockJavaActionHelper.call(new Controller1(),
                fakeRequest().bodyForm(ImmutableMap.of("email", "e", "password", "p")));
        assertThat(contentAsString(result), equalTo("e"));
    }

    public static class Controller1 extends MockJavaAction {
        public Result index() {
            Form<User> userForm = Form.form(User.class);
            //#bind-from-request
            User user = userForm.bindFromRequest().get();
            //#bind-from-request

            return ok(user.email);
        }
    }

    @Test
    public void constrants() {
        Form<javaguide.forms.u2.User> userForm = Form.form(javaguide.forms.u2.User.class);
        assertThat(userForm.bind(ImmutableMap.of("password", "p")).hasErrors(), equalTo(true));
    }

    @Test
    public void adhocValidation() {
        Form<javaguide.forms.u3.User> userForm = Form.form(javaguide.forms.u3.User.class);
        Form<javaguide.forms.u3.User> bound = userForm.bind(ImmutableMap.of("email", "e", "password", "p"));
        assertThat(bound.hasGlobalErrors(), equalTo(true));
        assertThat(bound.globalError().message(), equalTo("Invalid email or password"));

        // Run it through the template
        assertThat(javaguide.forms.html.view.render(bound).toString(), containsString("Invalid email or password"));
    }

    public static String authenticate(String email, String password) {
        return null;
    }

    @Test
    public void listValidation() {
        Form<UserForm> userForm = Form.form(UserForm.class).bind(ImmutableMap.of("email", "e"));
        assertThat(userForm.errors().get("email"), notNullValue());
        assertThat(userForm.errors().get("email").get(0).message(), equalTo("This e-mail is already registered."));

        // Run it through the template
        assertThat(javaguide.forms.html.view.render(userForm).toString(), containsString("<p>This e-mail is already registered.</p>"));
    }

    public static class UserForm {
        public static class User {
            public static String byEmail(String email) {
                return email;
            }
        }

        public String email;

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

    @Test
    public void handleErrors() {
        Result result = MockJavaActionHelper.call(new Controller2(), fakeRequest());
        assertThat(contentAsString(result), startsWith("Got user"));
    }

    public static class Controller2 extends MockJavaAction {
        static Pviews views = new Pviews();
        static class Pviews {
            Phtml html = new Phtml();
        }
        static class Phtml {
            Pform form = new Pform();
        }
        static class Pform {
            String render(Form<?> form) {
                return "rendered";
            }
        }

        public Result index() {
            Form<User> userForm = Form.form(User.class).bind(ImmutableMap.of("email", "e"));
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
        Form<javaguide.forms.u1.User> userForm = Form.form(javaguide.forms.u1.User.class);
        //#fill
        userForm = userForm.fill(new User("bob@gmail.com", "secret"));
        //#fill
        assertThat(userForm.field("email").value(), equalTo("bob@gmail.com"));
        assertThat(userForm.field("password").value(), equalTo("secret"));
    }

    @Test
    public void dynamicForm() {
        Result result = MockJavaActionHelper.call(new Controller3(),
                fakeRequest().bodyForm(ImmutableMap.of("firstname", "a", "lastname", "b")));
        assertThat(contentAsString(result), equalTo("Hello a b"));
    }

    public static class Controller3 extends MockJavaAction {
        //#dynamic
        public Result hello() {
            DynamicForm requestData = Form.form().bindFromRequest();
            String firstname = requestData.get("firstname");
            String lastname = requestData.get("lastname");
            return ok("Hello " + firstname + " " + lastname);
        }
        //#dynamic
    }

    @Test
    public void registerFormatter() {
        //#register-formatter
        Formatters.register(LocalTime.class, new SimpleFormatter<LocalTime>() {

            private Pattern timePattern = Pattern.compile(
                    "([012]?\\d)(?:[\\s:\\._\\-]+([0-5]\\d))?"
            );

            @Override
            public LocalTime parse(String input, Locale l) throws ParseException {
                Matcher m = timePattern.matcher(input);
                if (!m.find()) throw new ParseException("No valid Input", 0);
                int hour = Integer.valueOf(m.group(1));
                int min = m.group(2) == null ? 0 : Integer.valueOf(m.group(2));
                return new LocalTime(hour, min);
            }

            @Override
            public String print(LocalTime localTime, Locale l) {
                return localTime.toString("HH:mm");
            }

        });
        //#register-formatter

        Form<WithLocalTime> form = Form.form(WithLocalTime.class);
        WithLocalTime obj = form.bind(ImmutableMap.of("time", "23:45")).get();
        assertThat(obj.time, equalTo(new LocalTime(23, 45)));
        assertThat(form.fill(obj).field("time").value(), equalTo("23:45"));
    }

    public static class WithLocalTime {
        public LocalTime time;
    }

}
