/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase

//#startup-evolutions
import jakarta.inject.Inject
import jakarta.inject.Singleton
import play.api.db.evolutions.ApplicationEvolutions
import play.api.db.Database

@Singleton
class StartupDataLoader @Inject() (db: Database, evolutions: ApplicationEvolutions) {
  // Only access the schema once all evolutions have been applied. In DEV mode, applying them
  // in the browser reloads the application, which creates this component again.
  if (evolutions.upToDate) {
    db.withConnection { connection =>
      // e.g. insert some initial data
    }
  }
}
//#startup-evolutions
