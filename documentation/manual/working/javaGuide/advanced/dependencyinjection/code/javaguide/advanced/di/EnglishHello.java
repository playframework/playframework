/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di;

//#implemented-by
public class EnglishHello implements Hello {

    public String sayHello(String name) {
        return "Hello " + name;
    }
}
//#implemented-by
