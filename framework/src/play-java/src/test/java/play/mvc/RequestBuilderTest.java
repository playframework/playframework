/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.stream.javadsl.Source;
import play.api.Application;
import play.api.Play;
import play.api.inject.guice.GuiceApplicationBuilder;
import play.api.libs.typedmap.TypedKey;
import play.api.libs.typedmap.TypedKeyFactory;
import play.core.j.JavaContextComponents;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import org.junit.Test;

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
        final TypedKey<Long> NUMBER = TypedKeyFactory.create("number");
        final TypedKey<String> COLOR = TypedKeyFactory.create("color");

        RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");
        assertFalse(builder.getAttr(NUMBER).isPresent());
        assertFalse(builder.getAttr(COLOR).isPresent());

        Request req1 = builder.build();

        builder.putAttr(NUMBER, 6L);
        assertTrue(builder.getAttr(NUMBER).isPresent());
        assertFalse(builder.getAttr(COLOR).isPresent());
        Request req2 = builder.build();

        builder.putAttr(NUMBER, 70L);
        assertTrue(builder.getAttr(NUMBER).isPresent());
        assertFalse(builder.getAttr(COLOR).isPresent());
        Request req3 = builder.build();

        builder.putAttrs(NUMBER.bindValue(6L), COLOR.bindValue("blue"));
        assertTrue(builder.getAttr(NUMBER).isPresent());
        assertTrue(builder.getAttr(COLOR).isPresent());
        Request req4 = builder.build();

        builder.putAttrs(COLOR.bindValue("red"));
        assertTrue(builder.getAttr(NUMBER).isPresent());
        assertTrue(builder.getAttr(COLOR).isPresent());
        Request req5 = builder.build();

        assertFalse(req1.getAttr(NUMBER).isPresent());
        assertFalse(req1.getAttr(COLOR).isPresent());

        assertEquals(Optional.of(6L), req2.getAttr(NUMBER));
        assertEquals((Long) 6L, req2.attr(NUMBER));
        assertFalse(req2.getAttr(COLOR).isPresent());

        assertEquals(Optional.of(70L), req3.getAttr(NUMBER));
        assertEquals((Long) 70L, req3.attr(NUMBER));
        assertFalse(req3.getAttr(COLOR).isPresent());

        assertEquals(Optional.of(6L), req4.getAttr(NUMBER));
        assertEquals((Long) 6L, req4.attr(NUMBER));
        assertEquals(Optional.of("blue"), req4.getAttr(COLOR));
        assertEquals("blue", req4.attr(COLOR));

        assertEquals(Optional.of(6L), req5.getAttr(NUMBER));
        assertEquals((Long) 6L, req5.attr(NUMBER));
        assertEquals(Optional.of("red"), req5.getAttr(COLOR));
        assertEquals("red", req5.attr(COLOR));
    }

    @Test
    public void testFlash() {
        Application app = new GuiceApplicationBuilder().build();
        Play.start(app);
        JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);
        Context ctx = new Context(new RequestBuilder().flash("a","1").flash("b","1").flash("b","2"), contextComponents);
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

    @SuppressWarnings("deprecation")
    @Test
    public void testUsername() {
        final Request req1 =
            new RequestBuilder().uri("http://playframework.com/").build();
        final Request req2 = req1.withAttr(Security.USERNAME, "user2");
        final Request req3 = req1.withUsername("user3");
        final Request req4 =
                new RequestBuilder().uri("http://playframework.com/").username("user4").build();

        assertNull(req1.username());
        assertFalse(req1.getAttr(Security.USERNAME).isPresent());

        assertEquals("user2", req2.username());
        assertTrue(req2.getAttr(Security.USERNAME).isPresent());
        assertEquals("user2", req2.attr(Security.USERNAME));

        assertEquals("user3", req3.username());
        assertTrue(req3.getAttr(Security.USERNAME).isPresent());
        assertEquals("user3", req3.attr(Security.USERNAME));

        assertEquals("user4", req4.username());
        assertTrue(req4.getAttr(Security.USERNAME).isPresent());
        assertEquals("user4", req4.attr(Security.USERNAME));
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
        Http.MultipartFormData.DataPart dp = new Http.MultipartFormData.DataPart("hello", "world");
        final Request request = new RequestBuilder().uri("http://playframework.com/")
                .bodyMultipart(Collections.singletonList(dp), app.materializer())
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
