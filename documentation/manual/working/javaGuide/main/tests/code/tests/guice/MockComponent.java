/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.tests.guice;

// #mock-component
public class MockComponent implements Component {
    public String hello() {
        return "mock";
    }
}
// #mock-component
