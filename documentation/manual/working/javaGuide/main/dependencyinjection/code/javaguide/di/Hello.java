/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

// #implemented-by
import com.google.inject.ImplementedBy;

@ImplementedBy(EnglishHello.class)
public interface Hello {

  String sayHello(String name);
}
// #implemented-by
