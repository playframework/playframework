package play.data;

import javax.validation.*;
import javax.validation.metadata.*;

import java.util.*;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import play.libs.F;
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
    
    private final Class<T> backedType;
    private final Map<String,String> data;
    private final Map<String,List<ValidationError>> errors;
    private final Option<T> value;
    private final T blankInstance;
    
    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     */
    public Form(Class<T> clazz) {
        this(clazz, new HashMap<String,String>(), new HashMap<String,List<ValidationError>>(), None());
    }
    
    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz wrapped class
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value of type <code>T</code> if the form submission was successful
     */
    public Form(Class<T> clazz, Map<String,String> data, Map<String,List<ValidationError>> errors, Option<T> value) {
        this.backedType = clazz;
        this.data = data;
        this.errors = errors;
        this.value = value;
        try {
            blankInstance = backedType.newInstance();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public Form<T> bindFromRequest() {
        Map<String,String> data = new HashMap<String,String>();
        Map<String,String[]> urlFormEncoded = play.mvc.Controller.request().urlFormEncoded();
        for(String key: urlFormEncoded.keySet()) {
            String[] value = urlFormEncoded.get(key);
            if(value.length > 0) {
                data.put(key, value[0]);
            }
        }
        return bind(data);
    }
    
    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    public Form<T> bind(Map<String,String> data) {
        DataBinder dataBinder = new DataBinder(blankInstance);
        dataBinder.setValidator(new SpringValidatorAdapter(play.data.validation.Validation.getValidator()));
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        dataBinder.bind(new MutablePropertyValues(data));
        dataBinder.validate();
        BindingResult result = dataBinder.getBindingResult();
        
        if(result.hasErrors()) {
            Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
            for(FieldError error: result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                if(key.startsWith("target.")) {
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
            return new Form(backedType, data, errors, None());
        } else {
            String globalError = null;
            if(result.getTarget() != null) {
                try {
                    java.lang.reflect.Method v = result.getTarget().getClass().getMethod("validate");
                    globalError = (String)v.invoke(result.getTarget());
                } catch(Exception e) {}
            }
            if(globalError != null) {
                Map<String,List<ValidationError>> errors = new HashMap<String,List<ValidationError>>();
                errors.put("", new ArrayList<ValidationError>());
                errors.get("").add(new ValidationError("", globalError, new ArrayList()));
                return new Form(backedType, data, errors, None());
            }
            return new Form(backedType, data, errors, Some((T)result.getTarget()));
        }
    }
    
    /**
     * Retrieves the actual form data.
     */
    public Map<String,String> data() {
        return data;
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
        return new Form(backedType, new HashMap<String,String>(), new HashMap<String,ValidationError>(), Some(value));
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
        if(value.isDefined()) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(value.get());
            beanWrapper.setAutoGrowNestedPaths(true);
            Object oValue = beanWrapper.getPropertyValue(key);
            if(oValue != null) {
                fieldValue = play.data.format.Formatters.print(beanWrapper.getPropertyTypeDescriptor(key), oValue);
            }
        } else {
            if(data.containsKey(key)) {
                fieldValue = data.get(key);
            } 
        }
        
        // Error
        List<ValidationError> fieldErrors = errors.get(key);
        if(fieldErrors == null) {
            fieldErrors = new ArrayList<ValidationError>();
        }
        
        // Format
        T2<String,List<Object>> format = null;
        BeanWrapper beanWrapper = new BeanWrapperImpl(blankInstance);
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
                        format = T2(d.name(), attributes);
                    }
                }
            }
        } catch(NullPointerException e) {}

        
        // Constraints
        PropertyDescriptor property = play.data.validation.Validation.getValidator().getConstraintsForClass(backedType).getConstraintsForProperty(key);
        List<T2<String,List<Object>>> constraints = new ArrayList<T2<String,List<Object>>>();
        if(property != null) {
            constraints = Constraints.displayableConstraint(property.getConstraintDescriptors());
        }
        
        return new Field(key, constraints, format, fieldErrors, fieldValue);
    }
    
    public String toString() {
        return "Form";
    }
    
    /**
     * A form field.
     */
    public static class Field {
        
        private final String name;
        private final List<T2<String,List<Object>>> constraints;
        private final T2<String,List<Object>> format;
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
        public Field(String name, List<T2<String,List<Object>>> constraints, T2<String,List<Object>> format, List<ValidationError> errors, String value) {
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
        public List<T2<String,List<Object>>> constraints() {
            return constraints;
        }
        
        /**
         * Returns the expected format for this field.
         * 
         * @return The expected format for this field.
         */
        public T2<String,List<Object>> format() {
            return format;
        }
        
        public String toString() {
            return "Form.Field(" + name + ")";
        }
    
    }
    
}