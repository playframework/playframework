/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.models;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;

import play.libs.F;
import play.libs.F.*;
import play.mvc.QueryStringBindable;

// #declaration
public class CartItem implements QueryStringBindable<CartItem> {

  public String identifier;
  // #declaration

  @Override
  public Optional<CartItem> bind(String key, Map<String, String[]> data) {

    try {
      identifier = data.get("identifier")[0];
      return Optional.of(this);

    } catch (Exception e) { // no parameter match return None
      return Optional.empty();
    }
  }

  // #unbind
  @Override
  public String unbind(String key) {
    String identifierEncoded;
    try {
      identifierEncoded = URLEncoder.encode(identifier, "utf-8");
    } catch (Exception e) {
      // Should never happen
      identifierEncoded = identifier;
    }

    return new StringBuilder()
        // Key string doesn't contain special characters and doesn't need form URL encoding:
        .append("identifier")
        .append('=')
        // Value string may contain special characters, do encode:
        .append(identifierEncoded)
        .toString();
  }
  // #unbind

  @Override
  public String javascriptUnbind() {
    return new StringBuilder().append("identifier=").append(identifier).append(";").toString();
  }
}
