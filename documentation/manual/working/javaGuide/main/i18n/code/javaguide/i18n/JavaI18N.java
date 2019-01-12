/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.i18n;

import org.junit.Test;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.i18n.html.hellotemplate;
import javaguide.i18n.html.hellotemplateshort;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;

import java.util.*;

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
        MessagesApi messagesApi = instanceOf(MessagesApi.class);
        //#specify-lang-render
        String title = messagesApi.get(Lang.forCode("fr"), "hello");
        //#specify-lang-render

        assertTrue(title.equals("bonjour"));
    }

    @Test
    public void checkDefaultHello() {
        Result result = MockJavaActionHelper.call(new DefaultLangController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultLangController extends MockJavaAction {

        private final MessagesApi messagesApi;

        DefaultLangController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        //#default-lang-render
        public Result index(Http.Request request) {
            Messages messages = this.messagesApi.preferred(request);
            return ok(hellotemplate.render(messages));
        }
        //#default-lang-render
    }

    @Test
    public void checkDefaultScalaHello() {
        Result result = MockJavaActionHelper.call(new DefaultScalaLangController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultScalaLangController extends MockJavaAction {

        private final MessagesApi messagesApi;

        DefaultScalaLangController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        public Result index(Http.Request request) {
            Messages messages = this.messagesApi.preferred(request);
            return ok(hellotemplateshort.render(messages)); // "hello"
        }
    }

    @Test
    public void checkChangeLangHello() {
        Result result = MockJavaActionHelper.call(new ChangeLangController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("bonjour"));
    }

    @Test
    public void checkRequestMessages() {
        RequestMessagesController c = app.injector().instanceOf(RequestMessagesController.class);
        Result result = MockJavaActionHelper.call(c, fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class ChangeLangController extends MockJavaAction {

        private final MessagesApi messagesApi;

        ChangeLangController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        //#change-lang-render
        public Result index(Http.Request request) {
            Lang lang = Lang.forCode("fr");
            Messages messages = this.messagesApi.preferred(request.withTransientLang(lang));
            return ok(hellotemplate.render(messages)).withLang(lang, messagesApi);
        }
        //#change-lang-render
    }

    public static class RequestMessagesController extends MockJavaAction {

        @javax.inject.Inject
        public RequestMessagesController(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        @javax.inject.Inject
        private MessagesApi messagesApi;

        //#show-request-messages
        public Result index(Http.Request request) {
            Messages messages = this.messagesApi.preferred(request);
            String hello = messages.at("hello");
            return ok(hellotemplate.render(messages));
        }
        //#show-request-messages
    }

    @Test
    public void checkSetTransientLangHello() {
        Result result = MockJavaActionHelper.call(new SetTransientLangController(instanceOf(JavaHandlerComponents.class), instanceOf(MessagesApi.class)), fakeRequest("GET", "/"), mat);
        assertThat(contentAsString(result), containsString("howdy"));
    }

    public static class SetTransientLangController extends MockJavaAction {

        private final MessagesApi messagesApi;

        SetTransientLangController(JavaHandlerComponents javaHandlerComponents, MessagesApi messagesApi) {
            super(javaHandlerComponents);
            this.messagesApi = messagesApi;
        }

        //#set-transient-lang-render
        public Result index(Http.Request request) {
            Lang lang = Lang.forCode("en-US");
            Messages messages = this.messagesApi.preferred(request.withTransientLang(lang));
            return ok(hellotemplate.render(messages));
        }
        //#set-transient-lang-render
    }

    @Test
    public void testAcceptedLanguages() {
        Result result = MockJavaActionHelper.call(new AcceptedLanguageController(instanceOf(JavaHandlerComponents.class)), fakeRequest("GET", "/").header("Accept-Language", "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"), mat);
        assertThat(contentAsString(result), equalTo("fr-CH,fr,en,de"));
    }

    private static final class AcceptedLanguageController extends MockJavaAction {
        AcceptedLanguageController(JavaHandlerComponents javaHandlerComponents) {
            super(javaHandlerComponents);
        }

        // #accepted-languages
        public Result index(Http.Request request) {
            List<Lang> langs = request.acceptLanguages();
            String codes = langs.stream().map(Lang::code).collect(joining(","));
            return ok(codes);
        }
        // #accepted-languages
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

    // #explicit-messages-api
    private MessagesApi explicitMessagesApi() {
        return new play.i18n.MessagesApi(
                new play.api.i18n.DefaultMessagesApi(
                        Collections.singletonMap(Lang.defaultLang().code(), Collections.singletonMap("foo", "bar")),
                        new play.api.i18n.DefaultLangs().asJava())
        );
    }
    // #explicit-messages-api

    @Test
    public void testExplicitMessagesApi() {
        MessagesApi messagesApi = explicitMessagesApi();
        String message = messagesApi.get(Lang.defaultLang(), "foo");
        assertThat(message, equalTo("bar"));
    }

}
