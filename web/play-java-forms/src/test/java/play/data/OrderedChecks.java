/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import jakarta.validation.GroupSequence;

@GroupSequence({LoginCheck.class, PasswordCheck.class})
public interface OrderedChecks {}
