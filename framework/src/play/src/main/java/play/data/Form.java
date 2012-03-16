package play.data;

import javax.validation.*;
import javax.validation.metadata.*;

import java.util.*;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import play.libs.F;
import play.mvc.Http;
import static play.libs.F.*;

import play.data.validation.*;

import org.springframework.beans.*;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.*;

import org.hibernate.validator.engine.*;

/**
 * Helper to manage HTML form description, submission and validation.
 */
public class Form<T> {
    
    /**
     * Defines a form element's display name.
     */
    @Retention(RUNTIME)
    @Target({ANNOTATION_TYPE})
    public static @interface Display {
        String name();
        String[] attributes() default {};
    }
    
    // --
    
    private final String rootName;
    private final Class<T> backedType;
    private final Map<String,String> data;
    private final Map<String,List<ValidationError>> errors;
    private final Option<T> value;
    
    private T blankInstance() {
        try {
            return backedType.newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Cannot instantiate " + backedType + ". It must have a default constructor", e);
        }
    }
    
    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     */
    public Form(Class<T> clazz) {
        this(null, clazz);
    }
    
    public Form(String name, Class<T> clazz) {
        this(name, clazz, new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), None());
    }
    
    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     */
    public Form(String rootName, Class<T> clazz, Map<String,String> data, Map<String,List<ValidationError>> errors, Option<T> value) {
        this.rootName = rootName;
        this.backedType = clazz;
        this.data = data;
        this.errors = errors;
        this.value = value;
    }
    
    protected Map<String,String> requestData() {
        
        Map<String,String[]> urlFormEncoded = new HashMap<String,String[]>();
        if(play.mvc.Controller.request().body().asFormUrlEncoded() != null) {
            urlFormEncoded = play.mvc.Controller.request().body().asFormUrlEncoded();
        }
        
        Map<String,String[]> multipartFormData = new HashMap<String,String[]>();
        if(play.mvc.Controller.request().body().asMultipartFormData() != null) {
            multipartFormData = play.mvc.Controller.request().body().asMultipartFormData().asFormUrlEncoded();
        }
        
        Map<String,String> jsonData = new HashMap<String,String>();
        if(play.mvc.Controller.request().body().asJson() != null) {
            jsonData = play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("", 
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(play.mvc.Controller.request().body().asJson())
                    )
                )
            );
        }
        
        Map<String,String[]> queryString = play.mvc.Controller.request().queryString();
        
        Map<String,String> data = new HashMap<String,String>();
        
        for(String key: urlFormEncoded.keySet()) {
            String[] value = urlFormEncoded.get(key);
            if(value.length > 0) {
                data.put(key, value[0]);
            }
        }
        
        for(String key: multipartFormData.keySet()) {
            String[] value = multipartFormData.get(key);
            if(value.length > 0) {
                data.put(key, value[0]);
            }
        }
        
        for(String key: jsonData.keySet()) {
            data.put(key, jsonData.get(key));
        }
        
        for(String key: queryString.keySet()) {
            String[] value = queryString.get(key);
            if(value.length > 0) {
                data.put(key, value[0]);
            }
        }
        
        return data;
    }
    
    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest(String... allowedFields) {
        return bind(requestData(), allowedFields);
    }
    
    /**
     * Binds Json data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    public Form<T> bind(org.codehaus.jackson.JsonNode data, String... allowedFields) {
        return bind(
            play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("", 
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(data)
                    )
                )
            ),
            allowedFields
        );
    }
    
    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    public Form<T> bind(Map<String,String> data, String... allowedFields) {
        
        DataBinder dataBinder = null;
        Map<String, String> objectData = data;
        if(rootName == null) {
            dataBinder = new DataBinder(blankInstance());
        } else {
            dataBinder = new DataBinder(blankInstance(), rootName);
            objectData = new HashMap<String,String>();
            for(String key: data.keySet()) {
                if(key.startsWith(rootName + ".")) {
                    objectData.put(key.substring(rootName.length() + 1), data.get(key));
                }
            }
        }
        if(allowedFields.length > 0) {
            dataBinder.setAllowedFields(allowedFields);
        }
        dataBinder.setValidator(new SpringValidatorAdapter(play.data.validation.Validation.getValidator()));
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        dataBinder.bind(new MutablePropertyValues(objectData));
        dataBinder.validate();
        BindingResult result = dataBinder.getBindingResult();
        
        if(result.hasErrors()) {
            Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
            for(FieldError error: result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                if(key.startsWith("target.") && rootName == null) {
                    key = key.substring(7);
                }
                List<Object> arguments = new ArrayList<Object>();
                for(Object arg: error.getArguments()) {
                    if(!(arg instanceof org.springframework.context.support.DefaultMessageSourceResolvable)) {
                        arguments.add(arg);
                    }                    
                }
                if(!errors.containsKey(key)) {
                   errors.put(key, new ArrayList<ValidationError>()); 
                }
                errors.get(key).add(new ValidationError(key, error.isBindingFailure() ? "error.invalid" : error.getDefaultMessage(), arguments));                    
            }
            return new Form(rootName, backedType, data, errors, None());
        } else {
            Object globalError = null;
            if(result.getTarget() != null) {
                try {
                    java.lang.reflect.Method v = result.getTarget().getClass().getMethod("validate");
                    globalError = v.invoke(result.getTarget());
                } catch(NoSuchMethodException e) {
                } catch(Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            if(globalError != null) {
                Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
                if(globalError instanceof String) {
                    errors.put("", new ArrayList<ValidationError>());
                    errors.get("").add(new ValidationError("", (String)globalError, new ArrayList()));
                } else if(globalError instanceof List) {
                    errors.put("", (List<ValidationError>)globalError);
                } else if(globalError instanceof Map) {
                    errors = (Map<String,List<ValidationError>>)globalError;
                }
                return new Form(rootName, backedType, data, errors, None());
            }
            return new Form(rootName, backedType, new HashMap<String,String>(data), new HashMap<String,List<ValidationError>>(errors), Some((T)result.getTarget()));
        }
    }
    
    /**
     * Retrieves the actual form data.
     */
    public Map<String,String> data() {
        return data;
    }
    
    public String name() {
        return rootName;
    }
    
    /**
     * Retrieves the actual form value.
     */
    public Option<T> value() {
        return value;
    }
    
    /**
     * Populates this form with an existing value, used for edit forms.
     *
     * @param value existing value of type <code>T</code> used to fill this form
     * @return a copy of this form filled with the new data
     */
    public Form<T> fill(T value) {
        if(value == null) {
            throw new RuntimeException("Cannot fill a form with a null value");
        }
        return new Form(rootName, backedType, new HashMap<String,String>(), new HashMap<String,ValidationError>(), Some(value));
    }
    
    /**
     * Returns <code>true<code> if there are any errors related to this form.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns <code>true<code> if there any global errors related to this form.
     */
    public boolean hasGlobalErrors() {
        return errors.containsKey("") && !errors.get("").isEmpty();
    }
    
    /**
     * Retrieve all global errors - errors without a key.
     *
     * @return All global errors.
     */
    public List<ValidationError> globalErrors() {
        List<ValidationError> e = errors.get("");
        if(e == null) {
            e = new ArrayList<ValidationError>();
        }
        return e;
    }
    
    /**
     * Retrieves the first global error (an error without any key), if it exists.
     *
     * @return An error or <code>null</code>.
     */
    public ValidationError globalError() {
        List<ValidationError> errors = globalErrors();
        if(errors.isEmpty()) {
            return null;
        } else {
            return errors.get(0);
        }
    }
    
    /**
     * Returns all errors.
     *
     * @return All errors associated with this form.
     */
    public Map<String,List<ValidationError>> errors() {
        return errors;
    }
    
    /**
     * Retrieve an error by key.
     */
    public ValidationError error(String key) {
        List<ValidationError> err = errors.get(key);
        if(err == null || err.isEmpty()) {
            return null;
        } else {
            return err.get(0);
        }
    }
    
    /**
     * Returns the form errors serialized as Json.
     */
    public org.codehaus.jackson.JsonNode errorsAsJson() {
        return errorsAsJson(Http.Context.Implicit.lang());
    }
    
    /**
     * Returns the form errors serialized as Json using the given Lang.
     */
    public org.codehaus.jackson.JsonNode errorsAsJson(play.i18n.Lang lang) {
        Map<String, List<String>> allMessages = new HashMap<String, List<String>>();
        for (String key : errors.keySet()) {
            List<ValidationError> errs = errors.get(key);
            if (errs != null && !errs.isEmpty()) {
                List<String> messages = new ArrayList<String>();
                for (ValidationError error : errs) {
                    messages.add(play.i18n.Messages.get(lang, error.message(), error.arguments()));
                }
                allMessages.put(key, messages);
            }
        }
        return play.libs.Json.toJson(allMessages);
    }
    
    
    /**
     * Gets the concrete value if the submission was a success.
     */
    public T get() {
        return value.get();
    }
    
    /**
     * Adds an error to this form.
     *
     * @param error the <code>ValidationError</code> to add.
     */
    public void reject(ValidationError error) {
        if(!errors.containsKey(error.key())) {
           errors.put(error.key(), new ArrayList<ValidationError>()); 
        }
        errors.get(error.key()).add(error);
    }
    
    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     * @param args the errot arguments
     */
    public void reject(String key, String error, List<Object> args) {
        reject(new ValidationError(key, error, args));
    }

    /**
     * Adds an error to this form.
     *
     * @param key the error key
     * @param error the error message
     */    
    public void reject(String key, String error) {
        reject(key, error, new ArrayList());
    }
    
    /**
     * Adds a global error to this form.
     *
     * @param error the error message
     * @param args the errot arguments
     */
    public void reject(String error, List<Object> args) {
        reject(new ValidationError("", error, args));
    }

    /**
     * Add a global error to this form.
     *
     * @param error the error message.
     */    
    public void reject(String error) {
        reject("", error, new ArrayList());
    }
    
    /**
     * Retrieve a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field apply(String key) {
        return field(key);
    }
    
    /**
     * Retrieve a field.
     *
     * @param key field name
     * @return the field (even if the field does not exist you get a field)
     */
    public Field field(String key) {
        
        // Value
        String fieldValue = null;
        if(data.containsKey(key)) {
            fieldValue = data.get(key);
        } else {
            if(value.isDefined()) {
                BeanWrapper beanWrapper = new BeanWrapperImpl(value.get());
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = key;
                if(rootName != null && key.startsWith(rootName + ".")) {
                    objectKey = key.substring(rootName.length() + 1);
                }
                if(beanWrapper.isReadableProperty(objectKey)) {
                    Object oValue = beanWrapper.getPropertyValue(objectKey);
                    if(oValue != null) {
                        fieldValue = play.data.format.Formatters.print(beanWrapper.getPropertyTypeDescriptor(objectKey), oValue);
                    }
                }
            }
        }
        
        // Error
        List<ValidationError> fieldErrors = errors.get(key);
        if(fieldErrors == null) {
            fieldErrors = new ArrayList<ValidationError>();
        }
        
        // Format
        Tuple<String,List<Object>> format = null;
        BeanWrapper beanWrapper = new BeanWrapperImpl(blankInstance());
        beanWrapper.setAutoGrowNestedPaths(true);
        try {
            for(Annotation a: beanWrapper.getPropertyTypeDescriptor(key).getAnnotations()) {
                Class<?> annotationType = a.annotationType();
                if(annotationType.isAnnotationPresent(play.data.Form.Display.class)) {
                    play.data.Form.Display d = annotationType.getAnnotation(play.data.Form.Display.class);
                    if(d.name().startsWith("format.")) {
                        List<Object> attributes = new ArrayList<Object>();
                        for(String attr: d.attributes()) {
                            Object attrValue = null;
                            try {
                                attrValue = a.getClass().getDeclaredMethod(attr).invoke(a);
                            } catch(Exception e) {}
                            attributes.add(attrValue);
                        }
                        format = Tuple(d.name(), attributes);
                    }
                }
            }
        } catch(NullPointerException e) {}

        
        // Constraints
        PropertyDescriptor property = play.data.validation.Validation.getValidator().getConstraintsForClass(backedType).getConstraintsForProperty(key);
        List<Tuple<String,List<Object>>> constraints = new ArrayList<Tuple<String,List<Object>>>();
        if(property != null) {
            constraints = Constraints.displayableConstraint(property.getConstraintDescriptors());
        }
        
        return new Field(this, key, constraints, format, fieldErrors, fieldValue);
    }
    
    public String toString() {
        return "Form(of=" + backedType + ", data=" + data + ", value=" + value +", errors=" + errors + ")";
    }
    
    /**
     * A form field.
     */
    public static class Field {
        
        private final Form<?> form;
        private final String name;
        private final List<Tuple<String,List<Object>>> constraints;
        private final Tuple<String,List<Object>> format;
        private final List<ValidationError> errors;
        private final String value;
        
        /**
         * Creates a form field.
         *
         * @param name the field name
         * @param constraints the constraints associated with the field
         * @param format the format expected for this field
         * @param errors the errors associated with this field
         * @param value the field value ,if any
         */
        public Field(Form<?> form, String name, List<Tuple<String,List<Object>>> constraints, Tuple<String,List<Object>> format, List<ValidationError> errors, String value) {
            this.form = form;
            this.name = name;
            this.constraints = constraints;
            this.format = format;
            this.errors = errors;
            this.value = value;
        }
        
        /**
         * Returns the field name.
         *
         * @return The field name.
         */
        public String name() {
            return name;
        }
        
        /**
         * Returns the field value, if defined.
         *
         * @return The field value, if defined.
         */
        public String value() {
            return value;
        }
        
        public String valueOr(String or) {
            if(value == null) {
                return or;
            }
            return value;
        }
        
        /**
         * Returns all the errors associated with this field.
         *
         * @return The errors associated with this field.
         */
        public List<ValidationError> errors() {
            return errors;
        }
        
        /**
         * Returns all the constraints associated with this field.
         *
         * @return The constraints associated with this field.
         */
        public List<Tuple<String,List<Object>>> constraints() {
            return constraints;
        }
        
        /**
         * Returns the expected format for this field.
         * 
         * @return The expected format for this field.
         */
        public Tuple<String,List<Object>> format() {
            return format;
        }
        
        /**
         * Return the indexes available for this field (for repeated fields ad List)
         */
        public List<Integer> indexes() {
            List<Integer> result = new ArrayList<Integer>();
            if(form.value().isDefined()) {
                BeanWrapper beanWrapper = new BeanWrapperImpl(form.value().get());
                beanWrapper.setAutoGrowNestedPaths(true);
                String objectKey = name;
                if(form.name() != null && name.startsWith(form.name() + ".")) {
                    objectKey = name.substring(form.name().length() + 1);
                }
                if(beanWrapper.isReadableProperty(objectKey)) {
                    Object value = beanWrapper.getPropertyValue(objectKey);
                    if(value instanceof Collection) {
                        for(int i=0; i<((Collection)value).size(); i++) {
                            result.add(i);
                        }
                    }
                }
            } else {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(name) + "\\[(\\d+)\\].*$");
                for(String key: form.data().keySet()) {
                    java.util.regex.Matcher matcher = pattern.matcher(key);
                    if(matcher.matches()) {
                        result.add(Integer.parseInt(matcher.group(1)));
                    }
                }
            }
            return result;
        }
        
        /**
         * Get a sub-field, with a key relative to the current field.
         */
        public Field sub(String key) {
            String subKey = null;
            if(key.startsWith("[")) {
                subKey = name + key;
            } else {
                subKey = name + "." + key;
            }
            return form.field(subKey);
        }
        
        public String toString() {
            return "Form.Field(" + name + ")";
        }
    
    }
    
}