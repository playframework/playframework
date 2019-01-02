/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.payload;

//#interface
import play.db.Database;

import play.data.validation.Constraints.ValidationPayload;

public interface ValidatableWithDB<T> {
    public T validate(final Database db, final ValidationPayload payload);
}
//#interface
