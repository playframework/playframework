/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import com.typesafe.config.Config;
import java.util.Collections;
import java.util.List;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

public class OpenIdModule extends Module {

  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Collections.singletonList(bindClass(OpenIdClient.class).to(DefaultOpenIdClient.class));
  }
}
