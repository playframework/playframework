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

package play.data.internal.binding.validation;

import play.data.internal.binding.beans.ConfigurablePropertyAccessor;
import play.data.internal.binding.beans.PropertyAccessorUtils;
import play.data.internal.binding.core.convert.ConversionService;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.util.Assert;

/**
 * Abstract base class for {@link BindingResult} implementations that work with
 * the internal {@link play.data.internal.binding.beans.PropertyAccessor} mechanism.
 * Pre-implements field access through delegation to the corresponding
 * PropertyAccessor methods.
 *
 * @author Juergen Hoeller
 * @see #getPropertyAccessor()
 * @see play.data.internal.binding.beans.PropertyAccessor
 * @see play.data.internal.binding.beans.ConfigurablePropertyAccessor
 */
@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

	private transient ConversionService conversionService;


	/**
	 * Create a new AbstractPropertyBindingResult instance.
	 * @param objectName the name of the target object
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractPropertyBindingResult(String objectName) {
		super(objectName);
	}


	public void initConversion(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
		if (getTarget() != null) {
			getPropertyAccessor().setConversionService(conversionService);
		}
	}

	/**
	 * Returns the canonical property name.
	 * @see play.data.internal.binding.beans.PropertyAccessorUtils#canonicalPropertyName
	 */
	@Override
	protected String canonicalFieldName(String field) {
		return PropertyAccessorUtils.canonicalPropertyName(field);
	}

	/**
	 * Determines the field type from the property type.
	 * @see #getPropertyAccessor()
	 */
	@Override
	public Class<?> getFieldType(String field) {
		return (getTarget() != null ? getPropertyAccessor().getPropertyType(fixedField(field)) :
				super.getFieldType(field));
	}

	/**
	 * Fetches the field value from the PropertyAccessor.
	 * @see #getPropertyAccessor()
	 */
	@Override
	protected Object getActualFieldValue(String field) {
		return getPropertyAccessor().getPropertyValue(field);
	}

	@Override
	protected Object formatFieldValue(String field, Object value) {
		String fixedField = fixedField(field);
		if (this.conversionService != null) {
			TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
			TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
			if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
				return this.conversionService.convert(value, fieldDesc, strDesc);
			}
		}
		return value;
	}


	/**
	 * Provide the PropertyAccessor to work with, according to the
	 * concrete strategy of access.
	 */
	public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
