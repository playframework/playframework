package javaguide.sql;

import javax.inject.Inject;

import play.mvc.*;
import play.db.*;

class JavaApplicationDatabase extends Controller {
    @Inject Database db;
    // ...
}
