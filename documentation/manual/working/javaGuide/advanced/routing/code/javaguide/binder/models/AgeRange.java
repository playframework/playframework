/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.models;

import java.util.Map;
import java.util.Optional;
import play.libs.F;
import play.libs.F.*;
import play.mvc.QueryStringBindable;
// import java.util.Optional;

// #declaration
public class AgeRange implements QueryStringBindable<AgeRange> {

  public Integer from;
  public Integer to;
  // #declaration

  // #bind
  @Override
  public Optional<AgeRange> bind(String key, Map<String, String[]> data) {

    try {
      from = new Integer(data.get("from")[0]);
      to = new Integer(data.get("to")[0]);
      return Optional.of(this);

    } catch (Exception e) { // no parameter match return None
      return Optional.empty();
    }
  }

  @Override
  public String unbind(String key) {
    return new StringBuilder().append("from=").append(from).append("&to=").append(to).toString();
  }
  // #bind

  @Override
  public String javascriptUnbind() {
    return new StringBuilder().append("from=").append(from).append(";to=").append(to).toString();
  }
}
