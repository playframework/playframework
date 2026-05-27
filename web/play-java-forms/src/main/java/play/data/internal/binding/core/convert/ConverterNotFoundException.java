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

/**
 * Exception to be thrown when a suitable converter could not be found
 * in a given conversion service.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ConverterNotFoundException extends ConversionException {

	/**
	 * Create a new conversion executor not found exception.
	 * @param sourceType the source type requested to convert from
	 * @param targetType the target type requested to convert to
	 */
	public ConverterNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType) {
		super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
	}


}
