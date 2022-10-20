/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import com.typesafe.config.Config;
import java.util.Collections;
import java.util.List;
import play.Environment;
import play.inject.Binding;

// #module-class-definition
public class MyModule extends play.inject.Module {
  public List<Binding<?>> bindings(Environment environment, Config config) {
    return Collections.singletonList(bindClass(MyApi.class).toSelf());
  }
}
// #module-class-definition
