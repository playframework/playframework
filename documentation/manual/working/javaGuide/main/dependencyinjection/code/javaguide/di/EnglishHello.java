/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.di;

//#implemented-by
public class EnglishHello implements Hello {

    public String sayHello(String name) {
        return "Hello " + name;
    }
}
//#implemented-by
