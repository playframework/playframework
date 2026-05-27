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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import play.data.internal.binding.util.StringUtils;

/**
 * The default implementation of the {@link PropertyValues} interface.
 * Allows simple manipulation of properties, and provides constructors
 * to support deep copy and construction from a Map.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class MutablePropertyValues implements PropertyValues, Serializable {

	private static final PropertyValue[] EMPTY_PROPERTY_VALUE_ARRAY = new PropertyValue[0];


	private final List<PropertyValue> propertyValueList;

	/**
	 * Creates a new empty MutablePropertyValues object.
	 */
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList<>(0);
	}

	/**
	 * Deep copy constructor. Guarantees PropertyValue references
	 * are independent, although it can't deep copy objects currently
	 * referenced by individual PropertyValue objects.
	 * @param original the PropertyValues to copy
	 */
	public MutablePropertyValues(PropertyValues original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList<>(pvs.length);
			for (PropertyValue pv : pvs) {
				this.propertyValueList.add(new PropertyValue(pv));
			}
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * Construct a new MutablePropertyValues object from a Map.
	 * @param original a Map with property values keyed by property name Strings
	 */
	public MutablePropertyValues(Map<?, ?> original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			this.propertyValueList = new ArrayList<>(original.size());
			original.forEach((attrName, attrValue) -> this.propertyValueList.add(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}


	/**
	 * Return the underlying List of PropertyValue objects in its raw form.
	 * The returned List can be modified directly, although this is not recommended.
	 * <p>This is an accessor for optimized access to all PropertyValue objects.
	 * It is not intended for typical programmatic use.
	 */
	public List<PropertyValue> getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * Add a PropertyValue object, replacing any existing one for the
	 * corresponding property.
	 * @param pv the PropertyValue object to add
	 * @return this in order to allow for adding multiple property values in a chain
	 */
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * Modify a PropertyValue object held in this object.
	 * Indexed from 0.
	 */
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * Remove the given PropertyValue, if contained.
	 * @param pv the PropertyValue to remove
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	@Override
	public PropertyValue[] getPropertyValues() {
		return this.propertyValueList.toArray(EMPTY_PROPERTY_VALUE_ARRAY);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof MutablePropertyValues that &&
				this.propertyValueList.equals(that.propertyValueList)));
	}

	@Override
	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	@Override
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		if (pvs.length > 0) {
			return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
		}
		return "PropertyValues: length=0";
	}

}
