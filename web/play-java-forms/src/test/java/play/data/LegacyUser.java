/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.validation.Constraints.Validatable;

// No @Validate annotation here so we don't trigger the new validation mechanism.
// And because Validatable is implemented as well the legacy validation mechanism
// doesn't get triggered as well - so the validate() method here should NEVER run.
public class LegacyUser implements Validatable<String> {

    @Override
    public String validate() {
        return "Some global error";
    }

}
