/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data.validation;

import java.util.*;

import com.google.common.collect.ImmutableList;

/**
 * A form validation error.
 */
public class ValidationError {
    
    private String key;
    private List<String> messages;
    private List<Object> arguments;

    /**
     * Constructs a new <code>ValidationError</code>.
     *
     * @param key the error key
     * @param message the error message
     */
    public ValidationError(String key, String message) {
        this(key, message, ImmutableList.of());
    }
    
    /**
     * Constructs a new <code>ValidationError</code>.
     *
     * @param key the error key
     * @param message the error message
     * @param arguments the error message arguments
     */
    public ValidationError(String key, String message, List<Object> arguments) {
        this.key = key;
        this.arguments = arguments;
        this.messages = ImmutableList.of(message);
    }

    /**
     * Constructs a new <code>ValidationError</code>.
     *
     * @param key the error key
     * @param messages the list of error messages
     * @param arguments the error message arguments
     */
    public ValidationError(String key, List<String> messages, List<Object> arguments) {
        this.key = key;
        this.messages = messages;
        this.arguments = arguments;
    }

    /**
     * Returns the error key.
     */
    public String key() {
        return key;
    }

    /**
     * Returns the error message.
     */
    public String message() {
        return messages.get(messages.size()-1);
    }

    /**
     * Returns the error messages.
     */
    public List<String> messages() {
        return messages;
    }

    /**
     * Returns the error arguments.
     */
    public List<Object> arguments() {
        return arguments;
    }

    public String toString() {
        return "ValidationError(" + key + "," + messages + "," + arguments + ")";
    }

}
