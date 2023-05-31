/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.classfield;

// #class-field-dependency-injection
public class NoopCounter implements Counter {
  public void inc(String label) {}
}
// #class-field-dependency-injection
