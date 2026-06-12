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

package play.data.internal.binding.beans;

import play.data.internal.binding.core.convert.ConversionService;

/**
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor {

	/**
	 * Specify a {@link ConversionService} to use for converting property values.
	 */
	void setConversionService(ConversionService conversionService);

	/**
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain PropertyAccessor instance.
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * Specify a limit for array and collection auto-growing.
	 * <p>Default is unlimited on a plain PropertyAccessor instance.
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * Return the limit for array and collection auto-growing.
	 */
	int getAutoGrowCollectionLimit();

}
