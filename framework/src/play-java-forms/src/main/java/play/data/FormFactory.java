/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import play.i18n.MessagesApi;
import play.data.format.Formatters;

/**
 * Helper to create HTML forms.
 */
@Singleton
public class FormFactory {

    private final MessagesApi messagesApi;

    private final Formatters formatters;

    private final Validator validator;

    private final ObjectMapper objectMapper;

    @Inject
    public FormFactory(MessagesApi messagesApi, Formatters formatters, Validator validator, ObjectMapper mapper) {
        this.messagesApi = messagesApi;
        this.formatters = formatters;
        this.validator = validator;
        this.objectMapper = mapper;
    }

    /**
     * @return a dynamic form.
     */
    public DynamicForm form() {
        return new DynamicForm(messagesApi, formatters, validator, objectMapper);
    }

    /**
     * @param clazz    the class to map to a form.
     * @param <T>   the type of value in the form.
     * @return a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz) {
        return new Form<>(clazz, messagesApi, formatters, validator, objectMapper);
    }

    /**
     * @param <T>   the type of value in the form.
     * @param name the form's name.
     * @param clazz the class to map to a form.
     * @return a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz) {
        return new Form<>(name, clazz, messagesApi, formatters, validator, objectMapper);
    }

    /**
     * @param <T>   the type of value in the form.
     * @param name the form's name
     * @param clazz the class to map to a form.
     * @param groups the classes of groups.
     * @return a new form that wraps the specified class.
     */
    public <T> Form<T> form(String name, Class<T> clazz, Class<?>... groups) {
        return new Form<>(name, clazz, groups, messagesApi, formatters, validator, objectMapper);
    }

    /**
     * @param <T>   the type of value in the form.
     * @param clazz the class to map to a form.
     * @param groups the classes of groups.
     * @return a new form that wraps the specified class.
     */
    public <T> Form<T> form(Class<T> clazz, Class<?>... groups) {
        return new Form<>(null, clazz, groups, messagesApi, formatters, validator, objectMapper);
    }

}
