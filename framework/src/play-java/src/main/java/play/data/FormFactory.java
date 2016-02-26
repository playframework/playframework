/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import play.i18n.MessagesApi;
import play.data.format.Formatters;

/**
 * Helper to create HTML forms.
 */
@Singleton
public class FormFactory {

    private final MessagesApi messagesApi;

    private final Formatters formatters;

    @Inject
    public FormFactory(MessagesApi messagesApi, Formatters formatters) {
        this.messagesApi = messagesApi;
        this.formatters = formatters;
    }
    
    /**
     * Instantiates a dynamic form.
     */
    public DynamicForm form() {
        return new DynamicForm(messagesApi, formatters);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz) {
        return new Form<>(clazz, messagesApi, formatters);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz) {
        return new Form<>(name, clazz, messagesApi, formatters);
    }
    
    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz, Class<?> group) {
        return new Form<>(name, clazz, group, messagesApi, formatters);
    }

    /**
     * Instantiates a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz, Class<?> group) {
        return new Form<>(null, clazz, group, messagesApi, formatters);
    }

}
