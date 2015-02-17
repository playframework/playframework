/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di;

public class GermanHello implements Hello {
    @Override
    public String sayHello(String name) {
        return "Hallo " + name;
    }
}
