package play.data;

import java.util.*;

import play.libs.F;
import static play.libs.F.*;

import play.data.validation.*;

/**
 * A dynamic form. This form is backed by a simple HashMap&lt;String,String>
 */
public class DynamicForm extends Form<DynamicForm.Dynamic> {
    
    /**
     * Create a new empty dynamic form.
     */
    public DynamicForm() {
        super(DynamicForm.Dynamic.class);
    }
    
    /**
     * Create a new dynamic form.
     *
     * @param data The current form data (used to display the form).
     * @param errors The collection of errors associated with this form.
     * @param value Maybe a concrete value if the form submission was successful.
     */
    public DynamicForm(Map<String,String> data, Map<String,List<ValidationError>> errors, Option<Dynamic> value) {
        super(DynamicForm.Dynamic.class, data, errors, value);
    }
    
    /**
     * Get the concrete value if the submission was a success.
     */
    public String get(String key) {
        return (String)get().getData().get(key);
    }
    
    /**
     * Bind data coming from request to this form (ie. handle form submission).
     *
     * @return A copy of this form filled with the new data.
     */
    public DynamicForm bindFromRequest() {
        Form<Dynamic> form = super.bindFromRequest();
        return new DynamicForm(form.data(), form.errors(), form.value());
    }
    
    /**
     * Bind data to this form (ie. handle form submission).
     *
     * @param data Data to submit
     * @return A copy of this form filled with the new data.
     */
    public DynamicForm bind(Map<String,String> data) {
        Form<Dynamic> form = super.bind(data);
        return new DynamicForm(form.data(), form.errors(), form.value());
    }
    
    /**
     * Retrieve a field.
     *
     * @param key Field name.
     * @return The field (even of the field does not exist you get a field).
     */
    public Field field(String key) {
        return super.field("data[" + key + "]");
    }
    
    /**
     * Simple data structure used by DynamicForm.
     */
    public static class Dynamic {

        private Map data = new HashMap();

        /**
         * Retrieve the data.
         */
        public Map getData() {
            return data;
        }

        /**
         * Set the new data.
         */
        public void setData(Map data) {
            this.data = data;
        }

        public String toString() {
            return "Form.Dynamic(" + data.toString() + ")";
        }

    }
    
}

