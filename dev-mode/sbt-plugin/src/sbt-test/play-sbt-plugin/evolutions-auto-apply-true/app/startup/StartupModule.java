/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
