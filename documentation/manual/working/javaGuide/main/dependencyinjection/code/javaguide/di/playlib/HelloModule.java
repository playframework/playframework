/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.playlib;

import javaguide.di.*;

//#play-module
import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.List;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

public class HelloModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment environment, Config config) {
        return Arrays.asList(
            bindClass(Hello.class).qualifiedWith("en").to(EnglishHello.class),
            bindClass(Hello.class).qualifiedWith("de").to(GermanHello.class)
        );
    }
}
//#play-module
