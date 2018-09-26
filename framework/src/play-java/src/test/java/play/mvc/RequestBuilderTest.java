/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.javadsl.Source;
import org.junit.Test;
import play.api.Application;
import play.api.Play;
import play.api.inject.guice.GuiceApplicationBuilder;
import play.core.j.JavaContextComponents;
import play.libs.Files.TemporaryFileCreator;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class RequestBuilderTest {

    @Test
    public void testUri_absolute() {
        Request request = new RequestBuilder().uri("https://www.benmccann.com/blog").build();
        assertEquals("https://www.benmccann.com/blog", request.uri());
    }

    @Test
    public void testUri_relative() {
        Request request = new RequestBuilder().uri("/blog").build();
        assertEquals("/blog", request.uri());
    }

    @Test
    public void testUri_asterisk() {
        Request request = new RequestBuilder().method("OPTIONS").uri("*").build();
        assertEquals("*", request.uri());
    }

    @Test
    public void testSecure() {
        assertFalse(new RequestBuilder().uri("http://www.benmccann.com/blog").build().secure());
        assertTrue(new RequestBuilder().uri("https://www.benmccann.com/blog").build().secure());
    }

    @Test
    public void testAttrs() {
        final TypedKey<Long> NUMBER = TypedKey.create("number");
        final TypedKey<String> COLOR = TypedKey.create("color");

        RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");
        assertFalse(builder.attrs().containsKey(NUMBER));
        assertFalse(builder.attrs().containsKey(COLOR));

        Request req1 = builder.build();

        builder.attr(NUMBER, 6L);
        assertTrue(builder.attrs().containsKey(NUMBER));
        assertFalse(builder.attrs().containsKey(COLOR));

        Request req2 = builder.build();

        builder.attr(NUMBER, 70L);
        assertTrue(builder.attrs().containsKey(NUMBER));
        assertFalse(builder.attrs().containsKey(COLOR));

        Request req3 = builder.build();

        builder.attrs(builder.attrs().putAll(NUMBER.bindValue(6L), COLOR.bindValue("blue")));
        assertTrue(builder.attrs().containsKey(NUMBER));
        assertTrue(builder.attrs().containsKey(COLOR));

        Request req4 = builder.build();

        builder.attrs(builder.attrs().putAll(COLOR.bindValue("red")));
        assertTrue(builder.attrs().containsKey(NUMBER));
        assertTrue(builder.attrs().containsKey(COLOR));

        Request req5 = builder.build();

        assertFalse(req1.attrs().containsKey(NUMBER));
        assertFalse(req1.attrs().containsKey(COLOR));

        assertEquals(Optional.of(6L), req2.attrs().getOptional(NUMBER));
        assertEquals((Long) 6L, req2.attrs().get(NUMBER));
        assertFalse(req2.attrs().containsKey(COLOR));

        assertEquals(Optional.of(70L), req3.attrs().getOptional(NUMBER));
        assertEquals((Long) 70L, req3.attrs().get(NUMBER));
        assertFalse(req3.attrs().containsKey(COLOR));

        assertEquals(Optional.of(6L), req4.attrs().getOptional(NUMBER));
        assertEquals((Long) 6L, req4.attrs().get(NUMBER));
        assertEquals(Optional.of("blue"), req4.attrs().getOptional(COLOR));
        assertEquals("blue", req4.attrs().get(COLOR));

        assertEquals(Optional.of(6L), req5.attrs().getOptional(NUMBER));
        assertEquals((Long) 6L, req5.attrs().get(NUMBER));
        assertEquals(Optional.of("red"), req5.attrs().getOptional(COLOR));
        assertEquals("red", req5.attrs().get(COLOR));
    }

    @Test
    public void testFlash() {
        Application app = new GuiceApplicationBuilder().build();
        Play.start(app);
        JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);
        RequestBuilder builder = new RequestBuilder().flash("a","1").flash("b","1").flash("b","2");
        Context ctx = new Context(builder, contextComponents);
        assertEquals("1", ctx.flash().get("a"));
        assertEquals("2", ctx.flash().get("b"));
    }

    @Test
    public void testSession() {
        Application app = new GuiceApplicationBuilder().build();
        Play.start(app);
        JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);
        Context ctx = new Context(new RequestBuilder().session("a","1").session("b","1").session("b","2"), contextComponents);
        assertEquals("1", ctx.session().get("a"));
        assertEquals("2", ctx.session().get("b"));
        Play.stop(app);
    }

    @Test
    public void testUsername() {
        final Request req1 =
            new RequestBuilder().uri("http://playframework.com/").build();
        final Request req2 = req1.addAttr(Security.USERNAME, "user2");
        final Request req3 = req1.addAttr(Security.USERNAME, "user3");
        final Request req4 = new RequestBuilder().uri("http://playframework.com/").attr(Security.USERNAME, "user4").build();

        assertFalse(req1.attrs().containsKey(Security.USERNAME));

        assertTrue(req2.attrs().containsKey(Security.USERNAME));
        assertEquals("user2", req2.attrs().get(Security.USERNAME));

        assertTrue(req3.attrs().containsKey(Security.USERNAME));
        assertEquals("user3", req3.attrs().get(Security.USERNAME));

        assertTrue(req4.attrs().containsKey(Security.USERNAME));
        assertEquals("user4", req4.attrs().get(Security.USERNAME));
    }

    @Test
    public void testQuery_doubleEncoding() {
        final String query = new Http.RequestBuilder().uri("path?query=x%2By").build().getQueryString("query");
        assertEquals("x+y", query);
    }

    @Test
    public void testQuery_multipleParams() {
        final Request req = new Http.RequestBuilder().uri("/path?one=1&two=a+b&").build();
        assertEquals("1", req.getQueryString("one"));
        assertEquals("a b", req.getQueryString("two"));
    }

    @Test
    public void testQuery_emptyParam() {
        final Request req = new Http.RequestBuilder().uri("/path?one=&two=a+b&").build();
        assertEquals(null, req.getQueryString("one"));
        assertEquals("a b", req.getQueryString("two"));
    }

    @Test
    public void testUri_badEncoding() {
        final Request req = new Http.RequestBuilder().uri("/test.html?one=hello=world&two=false").build();
        assertEquals("hello=world", req.getQueryString("one"));
        assertEquals("false", req.getQueryString("two"));
    }

    @Test
    public void multipartForm() throws ExecutionException, InterruptedException {
        Application app = new GuiceApplicationBuilder().build();
        Play.start(app);
        TemporaryFileCreator temporaryFileCreator = app.injector().instanceOf(TemporaryFileCreator.class);
        Http.MultipartFormData.DataPart dp = new Http.MultipartFormData.DataPart("hello", "world");
        final Request request = new RequestBuilder().uri("http://playframework.com/")
                .bodyMultipart(Collections.singletonList(dp), temporaryFileCreator, app.materializer())
                .build();

        Optional<Http.MultipartFormData<File>> parts = app.injector().instanceOf(BodyParser.MultipartFormData.class)
               .apply(request)
               .run(Source.single(request.body().asBytes()), app.materializer())
               .toCompletableFuture()
               .get()
               .right;
        assertEquals(true, parts.isPresent());
        assertArrayEquals(new String[]{"world"}, parts.get().asFormUrlEncoded().get("hello"));

        Play.stop(app);
    }

}
