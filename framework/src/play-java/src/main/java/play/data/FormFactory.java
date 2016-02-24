/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;

import javax.inject.Inject;
import play.i18n.MessagesApi;

/**
 * Helper to create HTML forms.
 */
public class FormFactory {

    private final MessagesApi messagesApi;

    @Inject
    public FormFactory(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }
    
    /**
     * Instantiates a dynamic form.
     */
    public DynamicForm form() {
        return new DynamicForm(messagesApi);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz) {
        return new Form<>(clazz, messagesApi);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz) {
        return new Form<>(name, clazz, messagesApi);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz, Class<?> group) {
        return new Form<>(name, clazz, group, messagesApi);
    }

    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz, Class<?> group) {
        return new Form<>(null, clazz, group, messagesApi);
    }

}
