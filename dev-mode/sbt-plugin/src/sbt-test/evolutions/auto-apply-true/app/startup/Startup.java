/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package startup;

import play.db.Database;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Startup {

    @Inject
    public Startup(Database db) {
        // This works even without injecting play.api.db.evolutions.ApplicationEvolutions,
        // because it looks like ApplicationEvolutions gets initialized first.
        // Therefore the order in which (eager) components get initialzed matters.
        // Of course, it's always safer to just inject ApplicationEvolutions.
        controllers.UsersController.insertRow(db, "PlayerFromStartupInit");
    }
}
