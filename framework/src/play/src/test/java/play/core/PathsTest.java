/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PathsTest {
    /**
     * Current Path:    /playframework
     * Target Path:     /one
     * Relative Path:   one
     */
    @Test
    public void testRelative1() throws Throwable {
        final String startPath = "/playframework";
        final String targetPath = "/one";

        assertEquals("Relative path should return sibling path without common root",
                "one",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one/two/asset.js
     * Relative Path:   two/asset.js
     */
    @Test
    public void testRelative2() throws Throwable {
        final String startPath = "/one/two";
        final String targetPath = "/one/two/asset.js";

        assertEquals("Relative should return sibling path without common root",
                "two/asset.js",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one
     * Relative Path:   ../one
     */
    @Test
    public void testRelative3() throws Throwable {
        final String startPath = "/one/two";
        final String targetPath = "/one";

        assertEquals("Relative path should include one parent directory and last common element of target route with no trailing /",
                "../one",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two/
     * Target Path:     /one/
     * Relative Path:   ../
     */
    @Test
    public void testRelative4() throws Throwable {
        final String startPath = "/one/two/";
        final String targetPath = "/one/";

        assertEquals("Relative path should include one parent directory and no last common element",
                "../",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one-b/two-b
     * Relative Path:   ../one-b/two-b
     */
    @Test
    public void testRelative5() throws Throwable {
        final String startPath = "/one/two";
        final String targetPath = "/one-b/two-b";

        assertEquals("Relative path should include two parent directory",
                "../one-b/two-b",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two/three
     * Target Path:     /one-b/two-b/asset.js
     * Relative Path:   ../../one-b/two-b
     */
    @Test
    public void testRelative6() throws Throwable {
        final String startPath = "/one/two/three";
        final String targetPath = "/one-b/two-b/asset.js";

        assertEquals("Relative path should no common root segments and include three parent directories",
                "../../one-b/two-b/asset.js",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two/three/four
     * Target Path:     /one/two/three-b/four-b/asset.js
     * Relative Path:   "../three-b/four-b/asset.js
     */
    @Test
    public void testRelative7() throws Throwable {
        final String startPath = "/one/two/three/four";
        final String targetPath = "/one/two/three-b/four-b/asset.js";

        assertEquals("Relative path should have two common root segments and include two parent directories",
                "../three-b/four-b/asset.js",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two/
     * Target Path:     /one/two-c/
     * Relative Path:   two-c/
     */
    @Test
    public void testRelative8() throws Throwable {
        final String startPath = "/one/two";
        final String targetPath = "/one/two-c/";

        assertEquals("Relative path should retain trailing forward slash if it exists in Call",
                "two-c/",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Current Path:    /one/two
     * Target Path:     /one/two
     * Relative Path:   .
     */
    @Test
    public void testRelative9() throws Throwable {
        final String startPath = "/one/two";
        final String targetPath = "/one/two";

        assertEquals("Relative path return current dir",
                ".",
                Paths.relative(startPath, targetPath));
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
        final String startPath = "/one/two//three/../three-b/./four/";
        final String targetPath = "/one-b//two-b/./";

        assertEquals("Relative path return current dir",
                "../../../../one-b/two-b/",
                Paths.relative(startPath, targetPath));
    }

    /**
     * Path:            /one/two/../two-b/three
     * Canonical Path:  /one/two-b/three
     */
    @Test
    public void testCanonical1() throws Throwable {
        final String targetPath = "/one/two/../two-b/three";

        assertEquals("Canonical path return handles parent directories",
                "/one/two-b/three",
                Paths.canonical(targetPath));
    }

    /**
     * Path:            /one/two/./three
     * Canonical Path:  /one/two/three
     */
    @Test
    public void testCanonical2() throws Throwable {
        final String targetPath = "/one/two/./three";

        assertEquals("Canonical path handles current directories",
                "/one/two/three",
                Paths.canonical(targetPath));
    }

    /**
     * Path:            /one/two//three
     * Canonical Path:  /one/two/three
     */
    @Test
    public void testCanonical3() throws Throwable {
        final String targetPath = "/one/two//three";

        assertEquals("Canonical path handles multiple directory separators",
                "/one/two/three",
                Paths.canonical(targetPath));
    }
}
