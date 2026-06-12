/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified from the original Spring Framework source for Play Framework form binding by the Play Framework contributors.
 */

package play.data.internal.binding.context.i18n;

import java.util.Locale;

/**
 * Simple holder class that associates a Locale instance with the current thread.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 */
public final class LocaleContextHolder {

	private static final ThreadLocal<Locale> localeHolder = new ThreadLocal<>();

	private LocaleContextHolder() {
	}


	/**
	 * Reset the Locale for the current thread.
	 */
	public static void resetLocaleContext() {
		localeHolder.remove();
	}

	/**
	 * Associate the given Locale with the current thread.
	 * @param locale the current Locale, or {@code null} to reset
	 * the thread-bound context
	 */
	public static void setLocale(Locale locale) {
		if (locale != null) {
			localeHolder.set(locale);
		}
		else {
			resetLocaleContext();
		}
	}

	/**
	 * Return the Locale associated with the current thread, if any,
	 * or the configured default/system default Locale otherwise.
	 * @return the current Locale, or the system default Locale if no
	 * specific Locale has been associated with the current thread
	 * @see java.util.Locale#getDefault()
	 */
	public static Locale getLocale() {
		Locale locale = localeHolder.get();
		if (locale != null) {
			return locale;
		}
		return Locale.getDefault();
	}

}
