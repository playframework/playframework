/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.guice.classfield;

import com.google.inject.ImplementedBy;

// #class-field-dependency-injection
@ImplementedBy(LiveCounter.class)
interface Counter {
  public void inc(String label);
}
// #class-field-dependency-injection
