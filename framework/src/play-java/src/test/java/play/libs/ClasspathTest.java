/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import org.junit.Test;
import play.Environment;
import play.libs.testmodel.*;
import play.test.WithApplication;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ClasspathTest extends WithApplication {

    @Test
    public void testGetTypesApplication() {
        Set<String> set = Classpath.getTypes(app, "play.libs.testmodel");
        String[] result = {"play.libs.testmodel.AC1", "play.libs.testmodel.C1"};
        assertTrue(set.containsAll(Arrays.asList(result)));
    }

    @Test
    public void testGetTypesAnnotatedWithApplication() {
        Set set = Classpath.getTypesAnnotatedWith(app, "play.libs.testmodel", AC1.class);
        assertTrue(set.contains(C1.class));
    }

    @Test
    public void testGetTypesEnvironment() {
        Set<String> set = Classpath.getTypes(Environment.simple(), "play.libs.testmodel");
        String[] result = {"play.libs.testmodel.AC1", "play.libs.testmodel.C1"};
        assertTrue(set.containsAll(Arrays.asList(result)));
    }

    @Test
    public void testGetTypesAnnotatedWithEnvironment() {
        Set set = Classpath.getTypesAnnotatedWith(Environment.simple(), "play.libs.testmodel", AC1.class);
        assertTrue(set.contains(C1.class));
    }
}
