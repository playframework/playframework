/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import com.typesafe.config.Config;
import play.Environment;
import play.libs.Files;
import play.libs.concurrent.DefaultFutures;
import play.libs.concurrent.Futures;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCookieSigner;

import java.util.Arrays;
import java.util.List;

public class BuiltInModule extends Module {
    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Arrays.asList(
            bindClass(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
            bindClass(play.Environment.class).toSelf(),
            bindClass(CookieSigner.class).to(DefaultCookieSigner.class),
            bindClass(Files.TemporaryFileCreator.class).to(Files.DelegateTemporaryFileCreator.class),
            bindClass(Futures.class).to(DefaultFutures.class)
        );
    }
}
