/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class RunHooksRunner {

  public static void run(List<? extends RunHook> hooks, Consumer<RunHook> f) {
    run(hooks, f, false);
  }

  /** Runs all the hooks in the sequence of hooks. Reports last failure if any have failure. */
  public static void run(
      List<? extends RunHook> hooks, Consumer<RunHook> f, boolean suppressFailure) {
    try {
      Map<RunHook, Throwable> failures = new LinkedHashMap<>();
      hooks.forEach(
          hook -> {
            try {
              f.accept(hook);
            } catch (VirtualMachineError | ThreadDeath | LinkageError e) {
              throw e;
            } catch (Throwable e) {
              failures.put(hook, e);
            }
          });

      // Throw failure if it occurred....
      if (!suppressFailure && !failures.isEmpty()) {
        if (failures.size() == 1) {
          throw failures.values().iterator().next();
        } else {
          throw new RunHookCompositeThrowable(failures.values());
        }
      }
    } catch (VirtualMachineError | ThreadDeath | LinkageError e) {
      throw e;
    } catch (RuntimeException e) {
      if (!suppressFailure) throw e;
    } catch (Throwable e) {
      // Ignoring failure in running hooks... (CCE thrown here)
      if (!suppressFailure) throw new RuntimeException(e);
    }
  }

  private RunHooksRunner() {}
}
