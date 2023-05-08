/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package startup;

import play.api.db.evolutions.ApplicationEvolutions;
import play.db.Database;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Startup {

    @Inject
    public Startup(Database db, ApplicationEvolutions evolutions) {
        if(evolutions.upToDate()) {
            // When autoApply = false the only way to make this work is to add the upToDate check
            controllers.UsersController.insertRow(db, "PlayerFromStartupInit");
        }
    }
}
