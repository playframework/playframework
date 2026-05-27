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

package play.data.internal.binding.core.convert;

import play.data.internal.binding.util.ObjectUtils;

/**
 * Exception to be thrown when an actual type conversion attempt fails.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ConversionFailedException extends ConversionException {

	/**
	 * Create a new conversion exception.
	 * @param sourceType the value's original type
	 * @param targetType the value's target type
	 * @param value the value we tried to convert
	 * @param cause the cause of the conversion failure
	 */
	public ConversionFailedException(TypeDescriptor sourceType, TypeDescriptor targetType,
			Object value, Throwable cause) {

		super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
				"] for value [" + ObjectUtils.nullSafeConciseToString(value) + "]", cause);
	}


}
