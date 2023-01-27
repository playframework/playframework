/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedCache {
  String value();
}
