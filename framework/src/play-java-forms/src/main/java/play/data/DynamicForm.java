/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import javax.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

import play.data.validation.*;
import play.data.format.Formatters;
import play.i18n.MessagesApi;

/**
 * A dynamic form. This form is backed by a simple <code>HashMap&lt;String,String&gt;</code>
 */
public class DynamicForm extends Form<DynamicForm.Dynamic> {

    private final Map<String, String> rawData;
    
    /**
     * Creates a new empty dynamic form.
     *
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validator      the validator component.
     */
    public DynamicForm(MessagesApi messagesApi, Formatters formatters, Validator validator) {
        super(DynamicForm.Dynamic.class, messagesApi, formatters, validator);
        rawData = new HashMap<>();
    }
    
    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validator      the validator component.
     */
    public DynamicForm(Map<String,String> data, List<ValidationError> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, Validator validator) {
        super(null, DynamicForm.Dynamic.class, data, errors, value, messagesApi, formatters, validator);
        rawData = new HashMap<>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            rawData.put(asNormalKey(e.getKey()), e.getValue());
        }

    }

    /**
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validator      the validator component.
     * @deprecated Deprecated as of 2.6.0. Replace the parameter {@code Map<String,List<ValidationError>>} with a simple {@code List<ValidationError>}.
     */
    @Deprecated
    public DynamicForm(Map<String,String> data, Map<String,List<ValidationError>> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, Validator validator) {
        this(
                data,
                errors != null ? errors.values().stream().flatMap(v -> v.stream()).collect(Collectors.toList()) : new ArrayList<>(),
                value,
                messagesApi,
                formatters,
                validator
        );
    }
    
    /**
     * Gets the concrete value only if the submission was a success.
     * If the form is invalid because of validation errors this method will return null.
     * If you want to retrieve the value even when the form is invalid use {@link #value(String)} instead.
     * 
     * @param key the string key.
     * @return the value, or null if there is no match.
     */
    public String get(String key) {
        try {
            return (String)get().getData().get(asNormalKey(key));
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Gets the concrete value
     * @param key the string key.
     * @return the value
     */
    public Optional<Object> value(String key) {
        return super.value().map(v -> v.getData().get(asNormalKey(key)));
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public Map<String, String> data() {
        return rawData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> rawData() {
        return Collections.unmodifiableMap(rawData);
    }

    /**
     * Fills the form with existing data.
     * @param value    the map of values to fill in the form.
     * @return the modified form.
     */
    public DynamicForm fill(Map<String, Object> value) {
        Form<Dynamic> form = super.fill(new Dynamic(value));
        return new DynamicForm(new HashMap<>(form.rawData()), new ArrayList<>(form.allErrors()), form.value(), messagesApi, formatters, validator);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    @Override
    public DynamicForm bindFromRequest(String... allowedFields) {
        return bind(requestData(play.mvc.Controller.request()), allowedFields);
    }

    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    @Override
    public DynamicForm bindFromRequest(play.mvc.Http.Request request, String... allowedFields) {
        return bind(requestData(request), allowedFields);
    }

    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    @Override
    public DynamicForm bind(Map<String,String> data, String... allowedFields) {
        {
            Map<String,String> newData = new HashMap<>();
            for(Map.Entry<String, String> e: data.entrySet()) {
                newData.put(asDynamicKey(e.getKey()), e.getValue());
            }
            data = newData;
        }
        
        Form<Dynamic> form = super.bind(data, allowedFields);
        return new DynamicForm(new HashMap<>(form.rawData()), new ArrayList<>(form.allErrors()), form.value(), messagesApi, formatters, validator);
    }
    
    /**
     * Retrieves a field.
     *
     * @param key field name
     * @return the field - even if the field does not exist you get a field
     */
    public Form.Field field(String key) {
        // #1310: We specify inner class as Form.Field rather than Field because otherwise,
        // javadoc cannot find the static inner class.
        Field field = super.field(asDynamicKey(key));
        return new Field(this, key, field.constraints(), field.format(), field.errors(),
            field.getValue().orElse((String)value(key).orElse(null))
        );
    }

    /**
     * Retrieve an error by key.
     * 
     * @deprecated Deprecated as of 2.6.0. Use {@link #getError(String)} instead.
     */
    @Deprecated
    public ValidationError error(String key) {
        return super.error(asDynamicKey(key));
    }

    /**
     * Retrieve an error by key.
     */
    public Optional<ValidationError> getError(String key) {
        return super.getError(asDynamicKey(key));
    }

    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     * @param args the error arguments
     */
    public void reject(String key, String error, List<Object> args) {
        super.reject(asDynamicKey(key), error, args);
    }

    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     */    
    public void reject(String key, String error) {
        super.reject(asDynamicKey(key), error);
    }

    // -- tools

    static String asDynamicKey(String key) {
        if(key.isEmpty() || key.matches("^data\\[.+\\]$")) {
           return key;
        } else {
            return "data[" + key + "]";
        }
    }

    static String asNormalKey(String key) {
        if(key.matches("^data\\[.+\\]$")) {
           return key.substring(5, key.length() - 1);
        } else {
            return key;
        }
    }

    // -- /
    
    /**
     * Simple data structure used by <code>DynamicForm</code>.
     */
    public static class Dynamic {

        private Map<String, Object> data = new HashMap<>();

        public Dynamic() {
        }

        public Dynamic(Map<String, Object> data) {
            this.data = data;
        }

        /**
         * @return the data.
         */
        public Map<String, Object> getData() {
            return data;
        }

        /**
         * Sets the new data.
         * @param data    the map of data.
         */
        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public String toString() {
            return "Form.Dynamic(" + data.toString() + ")";
        }

    }
    
}

