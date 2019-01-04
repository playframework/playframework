/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.junit.Test;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.data.format.Formatters;
import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.ValidateWithPayload;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.ValidatableWithPayload;
import play.data.validation.Constraints.ValidationPayload;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.typedmap.TypedMap;
import play.mvc.*;
import play.mvc.Http.MultipartFormData.FilePart;
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
        //#create
        Form<User> userForm = formFactory.form(User.class);
        //#create

        Lang lang = new Lang(Locale.getDefault());
        TypedMap attrs = TypedMap.empty();
        FilePart<?> myProfilePicture = new FilePart<>("profilePicture", "me.jpg", "image/jpeg", null);
        //#bind
        Map<String,String> textData = new HashMap<>();
        textData.put("email", "bob@gmail.com");
        textData.put("password", "secret");

        Map<String, FilePart<?>> files = new HashMap<>();
        files.put("profilePicture", myProfilePicture);

        User user = userForm.bind(lang, attrs, textData, files).get();
        //#bind

        assertThat(user.getEmail(), equalTo("bob@gmail.com"));
        assertThat(user.getPassword(), equalTo("secret"));
        assertThat(user.getProfilePicture(), equalTo(myProfilePicture));
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

        public Result index(Http.Request request) {
            Form<User> userForm = formFactory().form(User.class);
            //#bind-from-request
            User user = userForm.bindFromRequest(request).get();
            //#bind-from-request

            return ok(user.getEmail());
        }
    }

    @Test
    public void constraints() {
        Form<javaguide.forms.u2.User> userForm = formFactory().form(javaguide.forms.u2.User.class);
        assertThat(userForm.bind(null, TypedMap.empty(), ImmutableMap.of("password", "p")).hasErrors(), equalTo(true));
    }

    @Test
    public void adhocValidation() {
        Result result = call(new U3UserController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of("email", "e", "password", "p")), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("Invalid email or password"));
    }

    public class U3UserController extends MockJavaAction {

        private final MessagesApi messagesApi;

        U3UserController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            Form<javaguide.forms.u3.User> userForm = formFactory().form(javaguide.forms.u3.User.class).bindFromRequest(request);
            Messages messages = this.messagesApi.preferred(request);

            if (userForm.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(userForm, messages));
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
        Result result = call(new ListValidationController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of("email", "e")), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("Access denied"));
        assertThat(contentAsString(result), containsString("Form could not be submitted"));
    }

    //#list-validate
    //###insert: import play.data.validation.Constraints.Validate;
    //###insert: import play.data.validation.Constraints.Validatable;
    //###insert: import play.data.validation.ValidationError;
    //###insert: import java.util.List;

    @Validate
    public static class SignUpForm implements Validatable<List<ValidationError>> {

        // fields, getters, setters, etc.

        //###skip: 19
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
    //#list-validate

    public class ListValidationController extends MockJavaAction {

        private final MessagesApi messagesApi;

        ListValidationController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            Form<SignUpForm> userForm = formFactory().form(SignUpForm.class).bindFromRequest(request);
            Messages messages = this.messagesApi.preferred(request);

            if (userForm.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(userForm, messages));
            } else {
                SignUpForm user = userForm.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void objectValidation() {
        Result result = call(new ObjectValidationController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of("email", "e")), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("Invalid credentials"));
    }

    //#object-validate
    //###insert: import play.data.validation.Constraints.Validate;
    //###insert: import play.data.validation.Constraints.Validatable;
    //###insert: import play.data.validation.ValidationError;

    @Validate
    public static class LoginForm implements Validatable<ValidationError> {

        // fields, getters, setters, etc.

        //###skip: 19
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
    //#object-validate

    public class ObjectValidationController extends MockJavaAction {

        private final MessagesApi messagesApi;

        ObjectValidationController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            Form<LoginForm> adminForm = formFactory().form(LoginForm.class).bindFromRequest(request);
            Messages messages = this.messagesApi.preferred(request);

            if (adminForm.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(adminForm, messages));
            } else {
                LoginForm user = adminForm.get();
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

        public Result index(Http.Request request) {
            Form<User> userForm = formFactory().form(User.class).bindFromRequest(request);
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
        assertThat(userForm.field("email").value().get(), equalTo("bob@gmail.com"));
        assertThat(userForm.field("password").value().get(), equalTo("secret"));
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
        public Result hello(Http.Request request) {
            DynamicForm requestData = formFactory.form().bindFromRequest(request);
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
        WithLocalTime obj = form.bind(null, TypedMap.empty(), ImmutableMap.of("time", "23:45")).get();
        assertThat(obj.getTime(), equalTo(LocalTime.of(23, 45)));
        assertThat(form.fill(obj).field("time").value().get(), equalTo("23:45"));
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

        //#validation-error-examples
        // Global error without internationalization:
        new ValidationError("", "Errors occurred. Please check your input!");
        // Global error; "validationFailed" should be defined in `conf/messages` - taking two arguments:
        new ValidationError("", "validationFailed", Arrays.asList(arg1, arg2));
        // Error for the email field; "emailUsedAlready" should be defined in `conf/messages` - taking the email as argument:
        new ValidationError("email", "emailUsedAlready", Arrays.asList(email));
        //#validation-error-examples
    }

    @Test
    public void partialFormSignupValidation() {
        Result result = call(new PartialFormSignupController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of()), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This field is required"));
    }

    public class PartialFormSignupController extends MockJavaAction {

        private final MessagesApi messagesApi;

        PartialFormSignupController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            //#partial-validate-signup
            Form<PartialUserForm> form = formFactory().form(PartialUserForm.class, SignUpCheck.class).bindFromRequest(request);
            //#partial-validate-signup

            Messages messages = this.messagesApi.preferred(request);

            if (form.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(form, messages));
            } else {
                PartialUserForm user = form.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void partialFormLoginValidation() {
        Result result = call(new PartialFormLoginController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of()), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This field is required"));
    }

    public class PartialFormLoginController extends MockJavaAction {

        private final MessagesApi messagesApi;

        PartialFormLoginController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }


        public Result index(Http.Request request) {
            //#partial-validate-login
            Form<PartialUserForm> form = formFactory().form(PartialUserForm.class, LoginCheck.class).bindFromRequest(request);
            //#partial-validate-login

            Messages messages = this.messagesApi.preferred(request);

            if (form.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(form, messages));
            } else {
                PartialUserForm user = form.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void partialFormDefaultValidation() {
        Result result = call(new PartialFormDefaultController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of()), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This field is required"));
    }

    public class PartialFormDefaultController extends MockJavaAction {

        private final MessagesApi messagesApi;

        PartialFormDefaultController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            //#partial-validate-default
            Form<PartialUserForm> form = formFactory().form(PartialUserForm.class, Default.class).bindFromRequest(request);
            //#partial-validate-default

            Messages messages = this.messagesApi.preferred(request);

            if (form.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(form, messages));
            } else {
                PartialUserForm user = form.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void partialFormNoGroupValidation() {
        Result result = call(new PartialFormNoGroupController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of()), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This field is required"));
    }

    public class PartialFormNoGroupController extends MockJavaAction {

        private final MessagesApi messagesApi;

        PartialFormNoGroupController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            //#partial-validate-nogroup
            Form<PartialUserForm> form = formFactory().form(PartialUserForm.class).bindFromRequest(request);
            //#partial-validate-nogroup

            Messages messages = this.messagesApi.preferred(request);

            if (form.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(form, messages));
            } else {
                PartialUserForm user = form.get();
                return ok("Got user " + user);
            }
        }
    }

    @Test
    public void OrderedGroupSequenceValidation() {
        Result result = call(new OrderedGroupSequenceController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("POST", "/")
                .bodyForm(ImmutableMap.of()), mat);

        // Run it through the template
        assertThat(contentAsString(result), containsString("This field is required"));
    }

    public class OrderedGroupSequenceController extends MockJavaAction {

        private final MessagesApi messagesApi;

        OrderedGroupSequenceController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            //#ordered-group-sequence-validate
            Form<PartialUserForm> form = formFactory().form(PartialUserForm.class, OrderedChecks.class).bindFromRequest(request);
            //#ordered-group-sequence-validate

            Messages messages = this.messagesApi.preferred(request);

            if (form.hasErrors()) {
                return badRequest(javaguide.forms.html.view.render(form, messages));
            } else {
                PartialUserForm user = form.get();
                return ok("Got user " + user);
            }
        }
    }

    //#payload-validate
    //###insert: import java.util.Map;

    //###insert: import com.typesafe.config.Config;

    //###insert: import play.data.validation.Constraints.ValidatableWithPayload;
    //###insert: import play.data.validation.Constraints.ValidateWithPayload;
    //###insert: import play.data.validation.ValidationError;
    //###insert: import play.data.validation.ValidationPayload;

    //###insert: import play.i18n.Lang;
    //###insert: import play.i18n.Messages;

    @ValidateWithPayload
    //###replace: public class ChangePasswordForm implements ValidatableWithPayload<ValidationError> {
    public static class ChangePasswordForm implements ValidatableWithPayload<ValidationError> {

        // fields, getters, setters, etc.

        @Override
        public ValidationError validate(ValidationPayload payload) {
            Lang lang = payload.getLang();
            Messages messages = payload.getMessages();
            //###insert:         Map<String, Object> ctxArgs = payload.getArgs();            //###insert:         Map<String, Object> ctxArgs = payload.getArgs();
            TypedMap attrs = payload.getAttrs();
            Config config = payload.getConfig();
            // ...
            //###skip: 1
            return null;
        }
    }
    //#payload-validate

}
