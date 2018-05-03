/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.libs.Files;
import play.libs.concurrent.DefaultFutures;
import play.libs.concurrent.Futures;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCookieSigner;
import play.mvc.FileMimeTypes;
import scala.collection.Seq;

public class BuiltInModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
            bind(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
            bind(play.Environment.class).toSelf(),
            bind(CookieSigner.class).to(DefaultCookieSigner.class),
            bind(Files.TemporaryFileCreator.class).to(Files.DelegateTemporaryFileCreator.class),
            bind(FileMimeTypes.class).toSelf(),
            bind(Futures.class).to(DefaultFutures.class)
        );
    }
}
