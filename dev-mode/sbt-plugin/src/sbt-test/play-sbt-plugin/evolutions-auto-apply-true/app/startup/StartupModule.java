/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package startup;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import java.util.Arrays;
import java.util.List;

public class StartupModule extends Module {

    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Arrays.asList(bindClass(Startup.class).toSelf().eagerly());
    }

}
