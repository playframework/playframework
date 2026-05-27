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

package play.data.internal.binding.context.support;

import java.io.Serializable;

import play.data.internal.binding.context.MessageSourceResolvable;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Default implementation of the {@link MessageSourceResolvable} interface.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

	private final String [] codes;

	private final Object [] arguments;

	private final String defaultMessage;


	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
		this(codes, null, defaultMessage);
	}

	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 * @param arguments the array of arguments to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(
			String [] codes, Object [] arguments, String defaultMessage) {

		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * Return the default code of this resolvable, that is,
	 * the last one in the codes array.
	 */
	public String getCode() {
		return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
	}

	@Override
	public String [] getCodes() {
		return this.codes;
	}

	@Override
	public Object [] getArguments() {
		return this.arguments;
	}

	@Override
	public String getDefaultMessage() {
		return this.defaultMessage;
	}

	/**
	 * Indicate whether the default message needs to be rendered.
	 */
	public boolean shouldRenderDefaultMessage() {
		return true;
	}


	/**
	 * Build a default String representation for this MessageSourceResolvable:
	 * including codes, arguments, and default message.
	 */
	protected final String resolvableToString() {
		StringBuilder result = new StringBuilder(64);
		result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
		result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
		result.append("]; default message [").append(this.defaultMessage).append(']');
		return result.toString();
	}

	/**
	 * The default implementation exposes the attributes of this MessageSourceResolvable.
	 * <p>To be overridden in more specific subclasses, potentially including the
	 * resolvable content through {@code resolvableToString()}.
	 * @see #resolvableToString()
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": " + resolvableToString();
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof MessageSourceResolvable that &&
				ObjectUtils.nullSafeEquals(getCodes(), that.getCodes()) &&
				ObjectUtils.nullSafeEquals(getArguments(), that.getArguments()) &&
				ObjectUtils.nullSafeEquals(getDefaultMessage(), that.getDefaultMessage())));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(getCode(), getArguments(), getDefaultMessage());
	}

}
