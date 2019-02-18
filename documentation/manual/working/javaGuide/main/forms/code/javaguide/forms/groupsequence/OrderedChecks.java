/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
