/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCSRFTokenSigner;
import play.libs.crypto.HMACSHA1CookieSigner;
import play.mvc.FileMimeTypes;
import scala.collection.Seq;

public class BuiltInModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
          bind(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
          bind(play.Configuration.class).toProvider(ConfigurationProvider.class),
          bind(CSRFTokenSigner.class).to(DefaultCSRFTokenSigner.class),
          bind(CookieSigner.class).to(HMACSHA1CookieSigner.class),
          bind(Files.TemporaryFileCreator.class).to(Files.DelegateTemporaryFileCreator.class),
          bind(FileMimeTypes.class).toSelf()
        );
    }
}
