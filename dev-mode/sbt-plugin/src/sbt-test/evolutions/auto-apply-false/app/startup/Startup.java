/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package startup;

import play.api.db.evolutions.ApplicationEvolutions;
import play.db.Database;

import javax.inject.Inject;
import javax.inject.Singleton;

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
