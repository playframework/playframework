/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.List;
import play.Environment;
import play.libs.Files;
import play.libs.concurrent.DefaultFutures;
import play.libs.concurrent.Futures;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCookieSigner;

public class BuiltInModule extends Module {
  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Arrays.asList(
        bindClass(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
        bindClass(CookieSigner.class).to(DefaultCookieSigner.class),
        bindClass(Files.TemporaryFileCreator.class).to(Files.DelegateTemporaryFileCreator.class),
        bindClass(Futures.class).to(DefaultFutures.class));
  }
}
