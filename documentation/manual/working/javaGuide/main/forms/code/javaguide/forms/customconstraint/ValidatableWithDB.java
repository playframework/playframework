/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint;

//#interface
import play.db.Database;

//import play.data.validation.Constraints.ValidatorPayload;

public interface ValidatableWithDB<T> {

    public T validate(final Database db);

    // or, if a payload is needed:
    // public T validate(final Database db, final ValidatorPayload payload);
}
//#interface
