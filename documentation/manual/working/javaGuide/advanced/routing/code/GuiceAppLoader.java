/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import play.ApplicationLoader;
import play.api.inject.BindingKey;
import play.api.inject.guice.GuiceableModule;
import play.api.inject.guice.GuiceableModule$;
import play.inject.guice.GuiceApplicationLoader;

// #load-guice1
public class GuiceAppLoader extends GuiceApplicationLoader {

  @Override
  protected GuiceableModule[] overrides(ApplicationLoader.Context context) {
    GuiceableModule[] modules = super.overrides(context);
    GuiceableModule module =
        GuiceableModule$.MODULE$.fromPlayBinding(
            new BindingKey<>(play.api.routing.Router.class)
                .toProvider(GuiceRouterProvider.class)
                .eagerly());

    List<GuiceableModule> copyModules = new ArrayList<>(Arrays.asList(modules));
    copyModules.add(module);

    return copyModules.toArray(new GuiceableModule[copyModules.size()]);
  }
}
// #load-guice1
