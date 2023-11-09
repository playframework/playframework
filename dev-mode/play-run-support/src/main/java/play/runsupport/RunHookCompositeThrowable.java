/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunHookCompositeThrowable extends Exception {

  public RunHookCompositeThrowable(Collection<Throwable> throwables) {
    super(
        "Multiple exceptions thrown during RunHook run: "
            + throwables.stream()
                .map(
                    t ->
                        Arrays.stream(t.getStackTrace())
                            .limit(10)
                            .map(Objects::toString)
                            .collect(Collectors.joining("\n", "\n", "\n...")))
                .collect(Collectors.joining("\n\n", throwables.toString(), "")));
  }
}
