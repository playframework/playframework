/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di;

// #singleton
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
// #singleton
