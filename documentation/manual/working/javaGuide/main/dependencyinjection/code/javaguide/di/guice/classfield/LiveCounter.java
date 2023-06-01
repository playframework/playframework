/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.classfield;

// #class-field-dependency-injection
import javax.inject.Singleton;

@Singleton
public class LiveCounter implements Counter {
  public void inc(String label) {
    System.out.println("inc " + label);
  }
}
// #class-field-dependency-injection
