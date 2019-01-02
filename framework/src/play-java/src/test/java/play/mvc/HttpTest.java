/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import play.Application;
import play.Environment;
import play.core.j.JavaContextComponents;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Http.RequestBuilder;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for the Http class. This test is in the play-java project
 * because we want to use some of the play-java classes, e.g.
 * the GuiceApplicationBuilder.
 */
public class HttpTest {

    /** Gets the PLAY_LANG cookie, or the last one if there is more than one */
    private String responseLangCookie(Context ctx, MessagesApi messagesApi) {
        String value = null;
        for (Cookie c : ctx.response().cookies()) {
          if (c.name().equals(messagesApi.langCookieName())) {
            value = c.value();
          }
        }
        return value;
    }

    private MessagesApi messagesApi(Application app) {
        return app.injector().instanceOf(MessagesApi.class);
    }

    private static Config addLangs(Environment environment) {
      Config langOverrides = ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]");
      Config loaded = ConfigFactory.load(environment.classLoader());
      return langOverrides.withFallback(loaded);
    }

    private static void withApplication(Consumer<Application> r) {
        Application app = new GuiceApplicationBuilder()
          .withConfigLoader(HttpTest::addLangs)
          .build();
        play.api.Play.start(app.asScala());
        try {
            r.accept(app);
        } finally {
            play.api.Play.stop(app.asScala());
        }
    }

    @Test
    public void testChangeLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            assertThat(ctx.changeLang("en-US")).isTrue();
            // The language and cookie should now be 'en-US'
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("en-US");
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
        });
    }

    @Test
    public void testMessagesOrder() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);
            Context ctx1 = new Context(new RequestBuilder().header(Http.HeaderNames.ACCEPT_LANGUAGE, "en-US"),
                    contextComponents);
            // if no cookie is provided the context lang order will have the accept language as the default lang
            assertThat(ctx1.messages().lang().code()).isEqualTo("en-US");

            Cookie cookie = Cookie.builder("PLAY_LANG", "fr").build();
            Context ctx2 = new Context(
                    new RequestBuilder().cookie(cookie).header(Http.HeaderNames.ACCEPT_LANGUAGE, "en"),
                    contextComponents);

            // if no context lang is provided the language order will be cookie > accept language
            assertThat(ctx2.messages().lang().code()).isEqualTo("fr");

            // if a context lang is set the language order will be context lang > cookie > accept language
            // Change the language to 'en-US'
            assertThat(ctx2.changeLang("en-US")).isTrue();
            // The messages language 'en-US'
            assertThat(ctx2.messages().lang().code()).isEqualTo("en-US");

            // check's that the order stays the same even when no cookie is changed
            // by using setTransientLang which will not set any cookie
            Context ctx3 = new Context(
                    new RequestBuilder().cookie(cookie).header(Http.HeaderNames.ACCEPT_LANGUAGE, "en"),
                    contextComponents);

            ctx3.setTransientLang("en-US");
            assertThat(ctx3.messages().lang().code()).isEqualTo("en-US");
        });
    }

    @Test
    public void testChangeLangFailure() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Try to change the language to 'en-NZ' - which will fail and return false
            assertThat(ctx.changeLang("en-NZ")).isFalse();
            // The language should still be 'en' and cookie should still be empty
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
        });
    }

    @Test
    public void testClearLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Set 'fr' as our initial language
            assertThat(ctx.changeLang("fr")).isTrue();
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("fr");
            // Clear language
            ctx.clearLang();
            // The language should now be 'en' and the cookie should be null
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("");
        });
    }

    @Test
    public void testSetTransientLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetTransientLangFailure() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Try to change the language to 'en-NZ' - which will throw an exception
            ctx.setTransientLang("en-NZ");
        });
    }

    @Test
    public void testClearTransientLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Cookie frCookie = new Cookie("PLAY_LANG", "fr", null, "/", null, false, false, null);
            RequestBuilder rb = new RequestBuilder().cookie(frCookie);
            Context ctx = new Context(rb, contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Clear the language to the default for the current request
            ctx.clearTransientLang();
            // The language should now be back to 'fr', and the cookie still mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
        });
    }

    @Test
    public void testCtxWithRequestLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);

            // Lets change the lang to something that is not the default
            ctx.setTransientLang("fr");

            // Make sure the context did set that lang correctly
            assertThat(ctx.lang().code()).isEqualTo("fr");

            // Now let's copy the context - only with a new request set, the rest should stay the same
            Context newCtx = ctx.withRequest(new RequestBuilder().build());

            // Make sure the new context correctly set its internal lang variable
            assertThat(newCtx.lang().code()).isEqualTo("fr");

            // Now change the lang on the new context to something not default
            newCtx.setTransientLang("en-US");

            // Make sure the new context correctly set its internal lang variable
            assertThat(newCtx.lang().code()).isEqualTo("en-US");
        });
    }

    @Test
    public void testWrappedCtxLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);

            // Lets change the lang to something that is not the default
            ctx.setTransientLang("fr");

            // Make sure the context did set that lang correctly
            assertThat(ctx.lang().code()).isEqualTo("fr");

            // Now let's copy the context - only with a new request set, the rest should stay the same
            Context newCtx = new Http.WrappedContext(ctx) {};

            // Make sure the new context correctly set its internal lang variable
            assertThat(newCtx.lang().code()).isEqualTo("fr");

            // Now change the lang on the new context to something not default
            newCtx.setTransientLang("en-US");

            // Make sure the new context correctly set its internal lang variable
            assertThat(newCtx.lang().code()).isEqualTo("en-US");
        });
    }

    @Test
    public void testTemplateMagicForJavaNoImplicitMessages() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

            ctx.changeLang("fr");

            try {
                Context.current.set(ctx);

                // Let's make sure french messages get returned from the context methods
                assertThat(Context.current().lang().code()).isEqualTo("fr");
                assertThat(Context.current().messages().at("bye")).isEqualTo("Au revoir!");

                Messages messages = messagesApi(app).preferred(Arrays.asList(new Lang(Locale.forLanguageTag("en-US"))));

                // Because the messages we pass to the view are not defined "implicit" the messages from the context will be used
                assertThat(NoImplicitMessages.render(messages).toString()).isEqualTo("Au revoir!");
            } finally {
                Context.current.remove();
            }
        });
    }

    @Test
    public void testTemplateMagicForJavaImplicitMessages() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

            ctx.changeLang("fr");

            try {
                Context.current.set(ctx);

                // Let's make sure french messages get returned from the context methods
                assertThat(Context.current().lang().code()).isEqualTo("fr");
                assertThat(Context.current().messages().at("bye")).isEqualTo("Au revoir!");

                Messages messages = messagesApi(app).preferred(Arrays.asList(new Lang(Locale.forLanguageTag("en-US"))));

                // Because we pass our own (implicit) messages to the view now the implicit PlayMagicForJava.implicitJavaMessages
                // should therefore have a lower weight and will not be used (resulting in the context messages being ignored)
                assertThat(ImplicitMessages.render(messages).toString()).isEqualTo("See you!");
            } finally {
                Context.current.remove();
            }
        });
    }

    @Test
    public void testTemplateMagicForJavaNoImplicitLang() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

            ctx.changeLang("fr");

            try {
                Context.current.set(ctx);

                // Let's make sure the french lang gets returned from the context methods
                assertThat(Context.current().lang().code()).isEqualTo("fr");

                Lang lang = new Lang(Locale.forLanguageTag("en-US"));

                // Because the lang we pass to the view is not defined "implicit" the lang from the context will be used
                assertThat(NoImplicitLang.render(lang).toString()).isEqualTo("fr");
            } finally {
                Context.current.remove();
            }
        });
    }

    @Test
    public void testTemplateMagicForJavaImplicitLang() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

            ctx.changeLang("fr");

            try {
                Context.current.set(ctx);

                // Let's make sure the french lang gets returned from the context methods
                assertThat(Context.current().lang().code()).isEqualTo("fr");

                Lang lang = new Lang(Locale.forLanguageTag("en-US"));

                // Because we pass our own (implicit) lang to the view now the implicit PlayMagicForJava.implicitJavaLang
                // should therefore have a lower weight and will not be used (resulting in the context lang being ignored)
                assertThat(ImplicitLang.render(lang).toString()).isEqualTo("en-US");
            } finally {
                Context.current.remove();
            }
        });
    }

    @Test
    public void testTemplateMagicForJavaNoImplicitRequest() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder().cookie(Cookie.builder("location", "contextrequest").build()), app.injector().instanceOf(JavaContextComponents.class));

            try {
                Context.current.set(ctx);

                // Let's make sure the request (and its cookie) is returned from the context methods
                assertThat(Context.current().request().cookie("location").value()).isEqualTo("contextrequest");

                Http.Request request = new RequestBuilder().cookie(Cookie.builder("location", "passedrequest").build()).build();

                // Because the request we pass to the view is not defined "implicit" the request (and therefore the cookie) from the context will be used
                assertThat(NoImplicitRequest.render(request).toString()).isEqualTo("contextrequest");
            } finally {
                Context.current.remove();
            }
        });
    }

    @Test
    public void testTemplateMagicForJavaImplicitRequest() {
        withApplication((app) -> {
            Context ctx = new Context(new RequestBuilder().cookie(Cookie.builder("location", "contextrequest").build()), app.injector().instanceOf(JavaContextComponents.class));

            try {
                Context.current.set(ctx);

                // Let's make sure the request (and its cookie) is returned from the context methods
                assertThat(Context.current().request().cookie("location").value()).isEqualTo("contextrequest");

                Http.Request request = new RequestBuilder().cookie(Cookie.builder("location", "passedrequest").build()).build();

                // Because we pass our own (implicit) request to the view now the implicit PlayMagicForJava.requestHeader
                // should therefore have a lower weight and will not be used (resulting in the context request being ignored)
                assertThat(ImplicitRequest.render(request).toString()).isEqualTo("passedrequest");
            } finally {
                Context.current.remove();
            }
        });
    }
}
