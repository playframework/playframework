/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.models;

import play.mvc.PathBindable;

// #declaration
public class User implements PathBindable<User> {

  public Long id;
  public String name;
  // #declaration

  // #bind
  @Override
  public User bindPath(String key, String id) {

    // findById meant to be lightweight operation
    User user = findById(Long.valueOf(id));
    if (user == null) {
      throw new IllegalArgumentException("User with id " + id + " not found");
    }
    return user;
  }

  @Override
  public String unbindPath(String key) {
    return String.valueOf(id);
  }
  // #bind

  @Override
  public String javascriptUnbindPath() {
    return "function(k,v) {\n" + "    return v.id;" + "}";
  }

  // stubbed test
  // designed to be lightweight operation
  private User findById(Long id) {
    if (id > 3) return null;
    User user = new User();
    user.id = id;
    user.name = "User " + String.valueOf(id);
    return user;
  }
}
