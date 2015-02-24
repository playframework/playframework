/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.tests.guice;

// #default-component
public class DefaultComponent implements Component {
    public String hello() {
        return "default";
    }
}
// #default-component
