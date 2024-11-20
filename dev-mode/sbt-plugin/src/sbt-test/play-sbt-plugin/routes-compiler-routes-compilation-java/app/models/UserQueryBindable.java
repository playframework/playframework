/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package models;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import play.mvc.QueryStringBindable;

public class UserQueryBindable extends User
    implements QueryStringBindable<UserQueryBindable> {

  public UserQueryBindable() {
    super(null);
  }

  public UserQueryBindable(String id) {
    super(id);
  }

  @Override
  public Optional<UserQueryBindable> bind(String key, Map<String, String[]> data) {
    return Optional.ofNullable(data.get(key))
        .filter(ids -> ids.length > 0 && !ids[0].isEmpty())
        .map(ids -> new UserQueryBindable(ids[0]));
  }

  @Override
  public String unbind(String key) {
    return URLEncoder.encode(key, UTF_8) + "=" + URLEncoder.encode(getId(), UTF_8);
  }

  @Override
  public String javascriptUnbind() {
    return "function(k,v) { return encodeURIComponent(k) + '=' + encodeURIComponent((v && 'id' in v) ? v['id'] : v) }";
  }
}
