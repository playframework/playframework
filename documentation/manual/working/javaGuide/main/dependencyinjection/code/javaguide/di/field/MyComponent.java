/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.field;

// #field
import javax.inject.*;
import play.libs.ws.*;

public class MyComponent {
  @Inject WSClient ws;

  // ...
}
// #field
