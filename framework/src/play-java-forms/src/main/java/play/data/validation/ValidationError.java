/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import java.util.*;

import com.google.common.collect.ImmutableList;
import play.i18n.Messages;

/**
 * A form validation error.
 */
public class ValidationError {
    
    private String key;
    private List<String> messages;
    private List<Object> arguments;

    /**
     * Constructs a new {@code ValidationError}.
     *
     * @param key the error key
     * @param message the error message
     */
    public ValidationError(String key, String message) {
        this(key, message, ImmutableList.of());
    }
    
    /**
     * Constructs a new {@code ValidationError}.
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
     * Constructs a new {@code ValidationError}.
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
     *
     * @return the error key of the message.
     */
    public String key() {
        return key;
    }

    /**
     * Returns the error message.
     *
     * @return the last message in the list of messages.
     */
    public String message() {
        return messages.get(messages.size()-1);
    }

    /**
     * Returns the error messages.
     *
     * @return a list of messages.
     */
    public List<String> messages() {
        return messages;
    }

    /**
     * Returns the error arguments.
     *
     * @return a list of error arguments.
     */
    public List<Object> arguments() {
        return arguments;
    }

    /**
     * Returns the formatted error message (message + arguments) in the given Messages.
     *
     * @param messagesObj the play.i18n.Messages object containing the language.
     * @return the results of messagesObj.at(messages, arguments).
     */
    public String format(Messages messagesObj) {
        return messagesObj.at(messages, arguments);
    }

    public String toString() {
        return "ValidationError(" + key + "," + messages + "," + arguments + ")";
    }

}
