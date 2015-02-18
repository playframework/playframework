/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di;

//#singleton
import javax.inject.*;

@Singleton
public class CurrentSharePrice {
    private volatile int price;

    public void set(int p) {
        price = p;
    }

    public int get() {
        return price;
    }
}
//#singleton
