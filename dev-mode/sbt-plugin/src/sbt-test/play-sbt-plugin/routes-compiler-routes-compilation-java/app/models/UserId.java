/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models;

import play.mvc.PathBindable;
import play.mvc.QueryStringBindable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class UserId implements PathBindable<UserId> {

  private final String id;

  public UserId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public UserId bind(String key, String txt) {
    return new UserId(txt);
  }

  @Override
  public String unbind(String key) {
    return id;
  }

  @Override
  public String javascriptUnbind() {
    return "";
  }

  public static class UserIdQueryParam extends UserId implements QueryStringBindable<UserIdQueryParam> {
    public UserIdQueryParam(String id) {
      super(id);
    }

    @Override
    public Optional<UserIdQueryParam> bind(String key, Map<String, String[]> data) {
      return Optional.ofNullable(data.get(key)).filter(ids -> ids.length > 0).map(ids -> new UserIdQueryParam(ids[0]));
    }

    @Override
    public String unbind(String key) {
      return key + "=" + URLEncoder.encode(getId(), StandardCharsets.UTF_8);
    }
  }
}
