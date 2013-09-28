/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package test;

import org.junit.Test;
import play.api.libs.Files;

public class CheckJUnitRunningTest {
    @Test
    public void performSomeSideEffectSoCanBeCheckedLater() {
       Files.writeFile(new java.io.File("target/junit-running"), "true");
    }
}
