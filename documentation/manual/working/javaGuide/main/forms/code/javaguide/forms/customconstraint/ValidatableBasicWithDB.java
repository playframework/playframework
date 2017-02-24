/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

//#interface
import play.data.validation.ValidationError;

import play.db.Database;

public interface ValidatableBasicWithDB {
    public ValidationError validateInstance(final Database db);
}
//#interface
