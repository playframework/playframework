/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import jakarta.validation.GroupSequence;

@GroupSequence({LoginCheck.class, PasswordCheck.class})
public interface OrderedChecks {}
