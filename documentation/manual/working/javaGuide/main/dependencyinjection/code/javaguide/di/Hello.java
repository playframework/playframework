/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

// #implemented-by
import com.google.inject.ImplementedBy;

@ImplementedBy(EnglishHello.class)
public interface Hello {

  String sayHello(String name);
}
// #implemented-by
