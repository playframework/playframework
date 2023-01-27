/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedDatabase {
  String value();
}
