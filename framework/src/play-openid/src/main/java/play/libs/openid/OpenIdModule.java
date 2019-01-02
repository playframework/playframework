/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import java.util.Collections;
import java.util.List;

public class OpenIdModule extends Module {

    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Collections.singletonList(
                bindClass(OpenIdClient.class).to(DefaultOpenIdClient.class)
        );
    }
}
