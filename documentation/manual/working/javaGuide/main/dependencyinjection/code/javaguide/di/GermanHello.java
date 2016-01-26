/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.di;

public class GermanHello implements Hello {
    @Override
    public String sayHello(String name) {
        return "Hallo " + name;
    }
}
