/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import com.google.inject.Singleton;
import java.lang.annotation.*;

/**
 * No scope; the same as not applying any scope at all. Each time the Injector obtains an instance
 * of an object with "no scope", it injects this instance then immediately forgets it. When the next
 * request for the same binding arrives it will need to obtain the instance over again.
 *
 * <p>This exists only in case a class has been annotated with a scope annotation such as {@link
 * Singleton @Singleton}, and you need to override this to "no scope" in your binding.
 */
@Target({ElementType.MODULE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoScope {}
