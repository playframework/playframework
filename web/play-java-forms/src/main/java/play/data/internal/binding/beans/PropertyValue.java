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

import java.io.Serializable;

import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ObjectUtils;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapperImpl} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see PropertyValues
 * @see BeanWrapperImpl
 */
@SuppressWarnings("serial")
public class PropertyValue implements Serializable {

	private final String name;

	private final Object value;

	private boolean converted = false;

	private Object convertedValue;

	/** Package-visible field that indicates whether conversion is necessary. */
	volatile Boolean conversionNecessary;

	/** Package-visible field for caching the resolved property path tokens. */
	transient volatile Object resolvedTokens;


	/**
	 * Create a new PropertyValue instance.
	 * @param name the name of the property (never {@code null})
	 * @param value the value of the property (possibly before type conversion)
	 */
	public PropertyValue(String name, Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 * @param original the PropertyValue to copy (never {@code null})
	 */
	public PropertyValue(PropertyValue original) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = original.getValue();
		this.converted = original.converted;
		this.convertedValue = original.convertedValue;
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
	}


	/**
	 * Return the name of the property.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the value of the property.
	 * <p>Note that type conversion will <i>not</i> have occurred here.
	 * It is the responsibility of the BeanWrapperImpl implementation to
	 * perform type conversion.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the original PropertyValue instance for this value holder.
	 * @return the original PropertyValue (either a source of this
	 * value holder or this value holder itself).
	 */
	public PropertyValue getOriginalPropertyValue() {
		return this;
	}

	/**
	 * Return whether this holder contains a converted value already ({@code true}),
	 * or whether the value still needs to be converted ({@code false}).
	 */
	public synchronized boolean isConverted() {
		return this.converted;
	}

	/**
	 * Return the converted value of this property value,
	 * after processed type conversion.
	 */
	public synchronized Object getConvertedValue() {
		return this.convertedValue;
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof PropertyValue that &&
				this.name.equals(that.name) &&
				ObjectUtils.nullSafeEquals(this.value, that.value)));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(this.name, this.value);
	}

	@Override
	public String toString() {
		return "bean property '" + this.name + "'";
	}

}
