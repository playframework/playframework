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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import play.data.internal.binding.beans.ConfigurablePropertyAccessor;
import play.data.internal.binding.beans.MutablePropertyValues;
import play.data.internal.binding.beans.PropertyAccessException;
import play.data.internal.binding.beans.PropertyAccessorUtils;
import play.data.internal.binding.beans.PropertyBatchUpdateException;
import play.data.internal.binding.beans.PropertyValue;
import play.data.internal.binding.beans.PropertyValues;
import play.data.internal.binding.core.convert.ConversionService;
import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.PatternMatchUtils;

/**
 * Binder that allows applying property values to a target object and supports
 * binding result analysis.
 *
 * <p>The binding process can be customized by specifying allowed field patterns.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore, the design and use of data binding should be considered
	 * carefully with regard to security.
 *
 * <p>The binding results can be examined via the {@link BindingResult} interface,
 * extending the {@link Errors} interface: see the {@link #getBindingResult()} method.
 * Property access exceptions will be converted to {@link FieldError FieldErrors},
 * collected in the Errors instance, using the following error codes:
 *
 * <ul>
 * <li>Type mismatch error: "typeMismatch"
 * <li>Method invocation error: "methodInvocation"
 * </ul>
 *
 * <p>By default, binding errors get resolved through the {@link DefaultBindingErrorProcessor}
 * strategy, processing property access exceptions.
 *
 * <p>Custom validation errors can be added afterwards. You will typically want to resolve
 * such error codes into proper user-visible error messages.
 * {@link DefaultMessageCodesResolver}'s javadoc states details on the
 * default resolution rules.
 *
 * <p>This generic data binder can be used in any kind of environment.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @see #setAllowedFields
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 */
public class DataBinder {

	/** Default object name used for binding: "target". */
	public static final String DEFAULT_OBJECT_NAME = "target";

	/** Default limit for bean property array and collection growing: 256. */
	public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;


	/**
	 * We'll create a lot of DataBinder instances: Let's use a static logger.
	 */
	protected static final Log logger = LogFactory.getLog(DataBinder.class);

	private Object target;

	private final String objectName;

	private AbstractPropertyBindingResult bindingResult;

	private boolean directFieldAccess = false;

	private boolean autoGrowNestedPaths = true;

	private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

	private String [] allowedFields;

	private ConversionService conversionService;

	private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();


	/**
	 * Create a new DataBinder instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public DataBinder(Object target) {
		this(target, DEFAULT_OBJECT_NAME);
	}

	/**
	 * Create a new DataBinder instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public DataBinder(Object target, String objectName) {
		this.target = ObjectUtils.unwrapOptional(target);
		this.objectName = objectName;
	}


	/**
	 * Return the wrapped target object.
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Return the name of the bound object.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Set whether this binder should attempt to "auto-grow" a nested path that contains a null value.
	 * <p>If "true", a null path location will be populated with a default object value and traversed
	 * instead of resulting in an exception. This flag also enables auto-growth of collection elements
	 * when accessing an out-of-bounds index.
	 * <p>Default is "true" on a standard DataBinder. This feature is supported for bean property access
	 * (DataBinder's default mode) and field access.
	 * <p>Used for binding via {@link #bind(PropertyValues)}.
	 * @see play.data.internal.binding.beans.ConfigurablePropertyAccessor#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * Specify the limit for array and collection auto-growing.
	 * <p>Default is 256, preventing OutOfMemoryErrors in case of large indexes.
	 * Raise this limit if your auto-growing needs are unusually high.
	 * @see play.data.internal.binding.beans.BeanWrapperImpl#setAutoGrowCollectionLimit
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * Return the current limit for array and collection auto-growing.
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * Create the {@link AbstractPropertyBindingResult} instance using standard
	 * JavaBean property access.
	 */
	protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}

		return result;
	}

	/**
	 * Initialize direct field access for this DataBinder,
	 * as alternative to the default bean property access.
	 * @see #createDirectFieldBindingResult()
	 */
	public void initDirectFieldAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
		this.directFieldAccess = true;
	}

	/**
	 * Create the {@link AbstractPropertyBindingResult} instance using direct
	 * field access.
	 */
	protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
		DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}

		return result;
	}

	/**
	 * Return the internal BindingResult held by this DataBinder,
	 * as an AbstractPropertyBindingResult.
	 */
	protected AbstractPropertyBindingResult getInternalBindingResult() {
		if (this.bindingResult == null) {
			this.bindingResult = (this.directFieldAccess ?
					createDirectFieldBindingResult(): createBeanPropertyBindingResult());
		}
		return this.bindingResult;
	}

	/**
	 * Return the underlying PropertyAccessor of this binder's BindingResult.
	 */
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		return getInternalBindingResult().getPropertyAccessor();
	}

	/**
	 * Return the BindingResult instance created by this DataBinder.
	 * This allows for convenient access to the binding results after
	 * a bind operation.
	 * @return the BindingResult instance, to be treated as BindingResult
	 * or as Errors instance (Errors is a super-interface of BindingResult)
	 * @see Errors
	 * @see #bind
	 */
	public BindingResult getBindingResult() {
		return getInternalBindingResult();
	}

	/**
	 * Register field patterns that should be allowed for binding.
	 * <p>Default is all fields.
	 * <p>Restrict this for example to avoid unwanted modifications by malicious
	 * users when binding HTTP request parameters.
	 * <p>Supports {@code "xxx*"}, {@code "*xxx"}, {@code "*xxx*"}, and
	 * {@code "xxx*yyy"} matches (with an arbitrary number of pattern parts), as
	 * well as direct equality.
	 * <p>The default implementation of this method stores allowed field patterns
	 * in {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) canonical}
	 * form. Subclasses which override this method must therefore take this into
	 * account.
	 * <p>More sophisticated matching can be implemented by overriding the
	 * {@link #isAllowed} method.
	 * <p>Used for binding to fields with {@link #bind(PropertyValues)}.
	 * @param allowedFields array of allowed field patterns
	 * @see #isAllowed(String)
	 */
	public void setAllowedFields(String ... allowedFields) {
		this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
	}

	/**
	 * Return the field patterns that should be allowed for binding.
	 * @return array of allowed field patterns
	 * @see #setAllowedFields(String...)
	 */
	public String [] getAllowedFields() {
		return this.allowedFields;
	}

	/**
	 * Set the strategy to use for processing binding errors.
	 * <p>Default is a DefaultBindingErrorProcessor.
	 * @see DefaultBindingErrorProcessor
	 */
	public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * Return the strategy for processing binding errors.
	 */
	public BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	public void setConversionService(ConversionService conversionService) {
		Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
		this.conversionService = conversionService;
		if (this.bindingResult != null && conversionService != null) {
			this.bindingResult.initConversion(conversionService);
		}
	}

	/**
	 * Return the associated ConversionService, if any.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Bind the given property values to this binder's target.
	 * <p>This call can create field errors, representing basic binding
	 * errors like a type mismatch between value and bean property
	 * (code "typeMismatch").
	 * <p>Note that the given PropertyValues should be a throwaway instance:
	 * For efficiency, it will be modified to just contain allowed fields if it
	 * implements the MutablePropertyValues interface; else, an internal mutable
	 * copy will be created for this purpose. Pass in a copy of the PropertyValues
	 * if you want your original instance to stay unmodified in any case.
	 * @param pvs property values to bind
	 * @see #doBind(play.data.internal.binding.beans.MutablePropertyValues)
	 */
	public void bind(PropertyValues pvs) {
		MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues mutablePropertyValues ?
				mutablePropertyValues : new MutablePropertyValues(pvs));
		doBind(mpvs);
	}

	/**
	 * Actual implementation of the binding process, working with the
	 * passed-in MutablePropertyValues instance.
	 * @param mpvs the property values to bind,
	 * as MutablePropertyValues instance
	 * @see #checkAllowedFields
	 * @see #applyPropertyValues
	 */
	protected void doBind(MutablePropertyValues mpvs) {
		checkAllowedFields(mpvs);
		applyPropertyValues(mpvs);
	}

	/**
	 * Check the given property values against the allowed fields,
	 * removing values for fields that are not allowed.
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getAllowedFields
	 * @see #isAllowed(String)
	 */
	protected void checkAllowedFields(MutablePropertyValues mpvs) {
		PropertyValue[] pvs = mpvs.getPropertyValues();
		for (PropertyValue pv : pvs) {
			String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
			if (!isAllowed(field)) {
				mpvs.removePropertyValue(pv);
				if (logger.isDebugEnabled()) {
					logger.debug("Field [" + field + "] has been removed from PropertyValues " +
							"and will not be bound, because it has not been found in the list of allowed fields");
				}
			}
		}
	}

	/**
	 * Determine if the given field is allowed for binding.
	 * <p>Invoked for each passed-in property value.
	 * <p>Checks for {@code "xxx*"}, {@code "*xxx"}, {@code "*xxx*"}, and
	 * {@code "xxx*yyy"} matches (with an arbitrary number of pattern parts),
	 * as well as direct equality, in the configured list of allowed field
	 * patterns.
	 * <p>Matching against allowed field patterns is case-sensitive.
	 * <p>Can be overridden in subclasses, but care must be taken to honor the
	 * aforementioned contract.
	 * @param field the field to check
	 * @return {@code true} if the field is allowed
	 * @see #setAllowedFields
	 * @see play.data.internal.binding.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isAllowed(String field) {
		String[] allowed = getAllowedFields();
		if (!ObjectUtils.isEmpty(allowed) && !PatternMatchUtils.simpleMatch(allowed, field)) {
			return false;
		}
		return true;
	}

	/**
	 * Apply given property values to the target object.
	 * <p>Default implementation applies all the supplied property
	 * values as bean property values. By default, unknown fields will
	 * be ignored.
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getTarget
	 * @see #getPropertyAccessor
	 * @see DefaultBindingErrorProcessor#processPropertyAccessException
	 */
	protected void applyPropertyValues(MutablePropertyValues mpvs) {
		try {
			// Bind request parameters onto target object.
			getPropertyAccessor().setPropertyValues(mpvs);
		}
		catch (PropertyBatchUpdateException ex) {
			// Use bind error processor to create FieldErrors.
			for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
				getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
			}
		}
	}

}
