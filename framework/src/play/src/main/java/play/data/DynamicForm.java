package play.data;

import java.util.*;

import play.libs.F;
import static play.libs.F.*;

import play.data.validation.*;

/**
 * A dynamic form. This form is backed by a simple <code>HashMap&lt;String,String></code>
 */
public class DynamicForm extends Form<DynamicForm.Dynamic> {
    
    /**
     * Creates a new empty dynamic form.
     */
    public DynamicForm() {
        super(DynamicForm.Dynamic.class);
    }
    
    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     */
    public DynamicForm(Map<String,String> data, Map<String,List<ValidationError>> errors, Option<Dynamic> value) {
        super(null, DynamicForm.Dynamic.class, data, errors, value);
    }
    
    /**
     * Gets the concrete value if the submission was a success.
     */
    public String get(String key) {
        return (String)get().getData().get(key);
    }
    
    /**
     * Binds request data to this form - that is, handles form submission.
     *
     * @return a copy of this form filled with the new data
     */
    public DynamicForm bindFromRequest() {
        return bind(requestData());
    }
    
    /**
     * Binds data to this form - that is, handles form submission.
     *
     * @param data data to submit
     * @return a copy of this form filled with the new data
     */
    public DynamicForm bind(Map<String,String> data) {
        
        {
            Map<String,String> newData = new HashMap<String,String>();
            for(String key: data.keySet()) {
                newData.put("data[" + key + "]", data.get(key));
            }
            data = newData;
        }
        
        Form<Dynamic> form = super.bind(data);
        return new DynamicForm(form.data(), form.errors(), form.value());
    }
    
    /**
     * Retrieves a field.
     *
     * @param key field name
     * @return the field - even if the field does not exist you get a field
     */
    public Field field(String key) {
        return super.field("data[" + key + "]");
    }
    
    /**
     * Simple data structure used by <code>DynamicForm</code>.
     */
    public static class Dynamic {

        private Map data = new HashMap();

        /**
         * Retrieves the data.
         */
        public Map getData() {
            return data;
        }

        /**
         * Sets the new data.
         */
        public void setData(Map data) {
            this.data = data;
        }

        public String toString() {
            return "Form.Dynamic(" + data.toString() + ")";
        }

    }
    
}

