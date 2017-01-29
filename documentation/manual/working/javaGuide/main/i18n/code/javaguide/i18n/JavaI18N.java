/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.i18n;

import org.junit.Test;
import org.junit.Before;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.i18n.html.hellotemplate;
import javaguide.i18n.html.helloscalatemplate;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;


public class JavaI18N extends WithApplication {

    @Override
    public Application provideApplication() {
        return fakeApplication(ImmutableMap.of(
            "play.i18n.langs", ImmutableList.of("en", "en-US", "fr"),
            "messages.path", "javaguide/i18n"
            ));
    }

    @Test
    public void checkSpecifyLangHello() {
        MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
        //#current-lang-render
        // Dependency inject with @Inject() public MyClass(MessagesApi messagesApi) { ... }
        Collection<Lang> candidates = Collections.singletonList(new Lang(Locale.US));
        Messages messages = messagesApi.preferred(candidates);
        String message = messages.at("home.title");
        //#current-lang-render
        //#specify-lang-render
        String title = messagesApi.get(Lang.forCode("fr"), "hello");
        //#specify-lang-render

        assertTrue(title.equals("bonjour"));
    }

    @Test
    public void checkDefaultHello() {
        Result result = MockJavaActionHelper.call(new DefaultLangController(), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultLangController extends MockJavaAction {
        //#default-lang-render
        public Result index() {
            return ok(hellotemplate.render()); // "hello"
        }
        //#default-lang-render
    }

    @Test
    public void checkDefaultScalaHello() {
        Result result = MockJavaActionHelper.call(new DefaultScalaLangController(), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultScalaLangController extends MockJavaAction {
        public Result index() {
            return ok(helloscalatemplate.render()); // "hello"
        }
    }

    @Test
    public void checkChangeLangHello() {
        Result result = MockJavaActionHelper.call(new ChangeLangController(), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("bonjour"));
    }

    @Test
    public void checkContextMessages() {
        ContextMessagesController c = app.injector().instanceOf(ContextMessagesController.class);
        Result result = MockJavaActionHelper.call(c, fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class ChangeLangController extends MockJavaAction {
        //#change-lang-render
        public Result index() {
            ctx().changeLang("fr");
            return ok(hellotemplate.render()); // "bonjour"
        }
        //#change-lang-render
    }

    public static class ContextMessagesController extends MockJavaAction {

        private final MessagesApi messagesApi;

        @javax.inject.Inject
        public ContextMessagesController(MessagesApi messagesApi) {
            this.messagesApi = messagesApi;
        }

        //#show-context-messages
        public Result index() {
            Messages messages = Http.Context.current().messages();
            String hello = messages.at("hello");
            return ok(hellotemplate.render());
        }
        //#show-context-messages
    }

    @Test
    public void checkSetTransientLangHello() {
        Result result = MockJavaActionHelper.call(new SetTransientLangController(), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("howdy"));
    }

    public static class SetTransientLangController extends MockJavaAction {
        //#set-transient-lang-render
        public Result index() {
            ctx().setTransientLang("en-US");
            return ok(hellotemplate.render()); // "howdy"
        }
        //#set-transient-lang-render
    }

    @Test
    public void testSingleApostrophe() {
        assertTrue(singleApostrophe());
    }

    private Boolean singleApostrophe() {
        MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
        Collection<Lang> candidates = Collections.singletonList(new Lang(Locale.US));
        Messages messages = messagesApi.preferred(candidates);
        //#single-apostrophe
        String errorMessage = messages.at("info.error");
        Boolean areEqual = errorMessage.equals("You aren't logged in!");
        //#single-apostrophe

        return areEqual;
    }

    @Test
    public void testEscapedParameters() {
        assertTrue(escapedParameters());
    }

    private Boolean escapedParameters() {
        MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
        Collection<Lang> candidates = Collections.singletonList(new Lang(Locale.US));
        Messages messages = messagesApi.preferred(candidates);
        //#parameter-escaping
        String errorMessage = messages.at("example.formatting");
        Boolean areEqual = errorMessage.equals("When using MessageFormat, '{0}' is replaced with the first parameter.");
        //#parameter-escaping

        return areEqual;
    }
}
