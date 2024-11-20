/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models;

import play.mvc.PathBindable;

public class UserPathBindable extends User implements PathBindable<UserPathBindable> {

  public UserPathBindable() {
    super(null);
  }

  public UserPathBindable(String id) {
    super(id);
  }

  @Override
  public UserPathBindable bind(String key, String value) {
    return new UserPathBindable(value);
  }

  @Override
  public String unbind(String key) {
    return getId();
  }

  @Override
  public String javascriptUnbind() {
    return "function(k,v) { return (v && 'id' in v) ? v['id'] : v; }";
  }
}
