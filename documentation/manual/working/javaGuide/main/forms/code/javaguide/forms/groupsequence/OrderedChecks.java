/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.groupsequence;

import javaguide.forms.groups.LoginCheck;
import javaguide.forms.groups.SignUpCheck;

// #ordered-checks
import javax.validation.GroupSequence;
import javax.validation.groups.Default;

@GroupSequence({Default.class, SignUpCheck.class, LoginCheck.class})
public interface OrderedChecks {}
// #ordered-checks
