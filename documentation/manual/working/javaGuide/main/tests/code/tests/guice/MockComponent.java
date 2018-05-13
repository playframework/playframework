/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests.guice;

// #mock-component
public class MockComponent implements Component {
    public String hello() {
        return "mock";
    }
}
// #mock-component
