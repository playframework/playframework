/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

import static org.junit.Assert.assertEquals;

public final class CallRelativeTest {
    /**
     * Current Path:    /playframework
     * Target Path:     /one
     * Relative Path:   one
     */
    @Test
    public void testRelative1() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/playframework").build();

        final TestCall call = new TestCall("/one", "GET");

        assertEquals("Relative path should return sibling path without common root",
                "one",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one/two/asset.js
     * Relative Path:   two/asset.js
     */
    @Test
    public void testRelative2() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one/two/asset.js", "GET");

        assertEquals("Relative should return sibling path without common root",
                "two/asset.js",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one
     * Relative Path:   ../one
     */
    @Test
    public void testRelative3() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one", "GET");

        assertEquals("Relative path should include one parent directory and last common element of target route with no trailing /",
                "../one",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two/
     * Target Path:     /one/
     * Relative Path:   ../
     */
    @Test
    public void testRelative4() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two/").build();

        final TestCall call = new TestCall("/one/", "GET");

        assertEquals("Relative path should include one parent directory and no last common element",
                "../",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one-b/two-b
     * Relative Path:   ../one-b/two-b
     */
    @Test
    public void testRelative5() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one-b/two-b", "GET");

        assertEquals("Relative path should include two parent directory",
                "../one-b/two-b",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two/three
     * Target Path:     /one-b/two-b/asset.js
     * Relative Path:   ../../one-b/two-b
     */
    @Test
    public void testRelative6() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two/three").build();

        final TestCall call = new TestCall("/one-b/two-b/asset.js", "GET");

        assertEquals("Relative path should no common root segments and include three parent directories",
                "../../one-b/two-b/asset.js",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two/three/four
     * Target Path:     /one/two/three-b/four-b/asset.js
     * Relative Path:   "../three-b/four-b/asset.js
     */
    @Test
    public void testRelative7() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two/three/four").build();

        final TestCall call = new TestCall("/one/two/three-b/four-b/asset.js", "GET");

        assertEquals("Relative path should have two common root segments and include two parent directories",
                "../three-b/four-b/asset.js",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two/
     * Target Path:     /one/two-c/
     * Relative Path:   two-c/
     */
    @Test
    public void testRelative8() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one/two-c/", "GET");

        assertEquals("Relative path should retain trailing forward slash if it exists in Call",
                "two-c/",
                call.relativeTo(req));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one/two
     * Relative Path:   .
     */
    @Test
    public void testRelative9() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one/two", "GET");

        assertEquals("Relative path return current dir",
                ".",
                call.relativeTo(req));
    }

    /**
     * Current Path:            /one/two//three/../three-b/./four/
     * Canonical Current Path:  /one/two/three-b/four/
     * Target Path:             /one-b//two-b/./
     * Canonical Target Path:   /one-b/two-b/
     * Relative Path:           ../../../../one-b/two-b/
     */
    @Test
    public void testRelative10() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two//three/../three-b/./four/").build();

        final TestCall call = new TestCall("/one-b//two-b/./", "GET");

        assertEquals("Relative path return current dir",
                "../../../../one-b/two-b/",
                call.relativeTo(req));
    }

    /**
     * Path:            /one/two/../two-b/three
     * Canonical Path:  /one/two-b/three
     */
    @Test
    public void testCanonical1() throws Throwable {
        final TestCall call = new TestCall("/one/two/../two-b/three", "GET");

        assertEquals("Canonical path return handles parent directories",
                "/one/two-b/three",
                call.canonical());
    }

    /**
     * Path:            /one/two/./three
     * Canonical Path:  /one/two/three
     */
    @Test
    public void testCanonical2() throws Throwable {
        final TestCall call = new TestCall("/one/two/./three", "GET");

        assertEquals("Canonical path handles current directories",
                "/one/two/three",
                call.canonical());
    }

    /**
     * Path:            /one/two//three
     * Canonical Path:  /one/two/three
     */
    @Test
    public void testCanonical3() throws Throwable {
        final TestCall call = new TestCall("/one/two//three", "GET");

        assertEquals("Canonical path handles multiple directory separators",
                "/one/two/three",
                call.canonical());
    }
}
