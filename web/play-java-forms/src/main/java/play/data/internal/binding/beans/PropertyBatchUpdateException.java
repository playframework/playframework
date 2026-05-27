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

import java.util.StringJoiner;

import play.data.internal.binding.util.Assert;

/**
 * Combined exception, composed of individual PropertyAccessException instances.
 * An object of this class is created at the beginning of the binding
 * process, and errors added to it as necessary.
 *
 * <p>The binding process continues when it encounters application-level
 * PropertyAccessExceptions, applying those changes that can be applied
 * and storing rejected changes in an object of this class.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class PropertyBatchUpdateException extends BeansException {

	/** List of PropertyAccessException objects. */
	private final PropertyAccessException[] propertyAccessExceptions;


	/**
	 * Create a new PropertyBatchUpdateException.
	 * @param propertyAccessExceptions the List of PropertyAccessExceptions
	 */
	public PropertyBatchUpdateException(PropertyAccessException[] propertyAccessExceptions) {
		super(null, null);
		Assert.notEmpty(propertyAccessExceptions, "At least 1 PropertyAccessException required");
		this.propertyAccessExceptions = propertyAccessExceptions;
	}


	/**
	 * Return an array of the propertyAccessExceptions stored in this object.
	 * <p>Will return the empty array (not {@code null}) if there were no errors.
	 */
	public final PropertyAccessException[] getPropertyAccessExceptions() {
		return this.propertyAccessExceptions;
	}


	@Override
	public String getMessage() {
		StringJoiner stringJoiner = new StringJoiner("; ", "Failed properties: ", "");
		for (PropertyAccessException exception : this.propertyAccessExceptions) {
			stringJoiner.add(exception.getMessage());
		}
		return stringJoiner.toString();
	}

}
