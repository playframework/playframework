/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;

import javax.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import play.data.format.Formatters;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;

/**
 * A dynamic form. This form is backed by a simple <code>HashMap&lt;String,String&gt;</code>
 */
public class DynamicForm extends Form<DynamicForm.Dynamic> {

    /** Statically compiled Pattern for checking if a key is already surrounded by "data[]". */
    private static final Pattern MATCHES_DATA = Pattern.compile("^data\\[.+\\]$");

    /**
     * Creates a new empty dynamic form.
     *
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validatorFactory      the validatorFactory component.
     * @param config      the config component.
     */
    public DynamicForm(MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        super(DynamicForm.Dynamic.class, messagesApi, formatters, validatorFactory, config);
    }

    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validatorFactory      the validatorFactory component.
     * @param config      the config component.
     */
    public DynamicForm(Map<String,String> data, List<ValidationError> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(data, Collections.emptyMap(), errors, value, messagesApi, formatters, validatorFactory, config);
    }

    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param files the current form file data
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validatorFactory      the validatorFactory component.
     * @param config      the config component.
     */
    public DynamicForm(Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config) {
        this(data, files, errors, value, messagesApi, formatters, validatorFactory, config, null);
    }

    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validatorFactory      the validatorFactory component.
     * @param config      the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     */
    public DynamicForm(Map<String,String> data, List<ValidationError> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang) {
        this(data, Collections.emptyMap(), errors, value, messagesApi, formatters, validatorFactory, config, lang);
    }

    /**
     * Creates a new dynamic form.
     *
     * @param data the current form data (used to display the form)
     * @param files the current form file data
     * @param errors the collection of errors associated with this form
     * @param value optional concrete value if the form submission was successful
     * @param messagesApi    the messagesApi component.
     * @param formatters     the formatters component.
     * @param validatorFactory      the validatorFactory component.
     * @param config      the config component.
     * @param lang used for formatting when retrieving a field (via {@link #field(String)} or {@link #apply(String)}) and for translations in {@link #errorsAsJson()}
     */
    public DynamicForm(Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, List<ValidationError> errors, Optional<Dynamic> value, MessagesApi messagesApi, Formatters formatters, ValidatorFactory validatorFactory, Config config, Lang lang) {
        super(null, DynamicForm.Dynamic.class, data, files, errors, value, null, messagesApi, formatters, validatorFactory, config, lang);
    }

    /**
     * Gets the concrete value only if the submission was a success.
     * If the form is invalid because of validation errors or you try to access a file field this method will return null.
     * If you want to retrieve the value even when the form is invalid use {@link #value(String)} instead.
     * If you want to retrieve a file field use {@link #file(String)} instead.
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
     * Gets the concrete value only if the submission was a success.
     * If the form is invalid because of validation errors or you try to access a non-file field this method will return null.
     * If you want to retrieve the value even when the form is invalid use {@link #value(String)} instead.
     * If you want to retrieve a non-file field use {@link #get(String)} instead.
     *
     * @param key the string key.
     * @return the value, or null if there is no match.
     */
    public <A> Http.MultipartFormData.FilePart<A> file(String key) {
        try {
            return (Http.MultipartFormData.FilePart<A>)get().getData().get(asNormalKey(key));
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

    @Override
    public Map<String, String> rawData() {
        return Collections.unmodifiableMap(super.rawData().entrySet().stream().collect(Collectors.toMap(e -> asNormalKey(e.getKey()), e -> e.getValue())));
    }

    @Override
    public Map<String, Http.MultipartFormData.FilePart<?>> files() {
        return Collections.unmodifiableMap(super.files().entrySet().stream().collect(Collectors.toMap(e -> asNormalKey(e.getKey()), e -> e.getValue())));
    }

    /**
     * Fills the form with existing data.
     * @param value    the map of values to fill in the form.
     * @return the modified form.
     */
    public DynamicForm fill(Map<String, Object> value) {
        Form<Dynamic> form = super.fill(new Dynamic(value));
        return new DynamicForm(form.rawData(), form.files(), form.errors(), form.value(), messagesApi, formatters, validatorFactory, config, lang().orElse(null));
    }

    @Override
    @Deprecated
    public DynamicForm bindFromRequest(String... allowedFields) {
        return bind(play.mvc.Controller.ctx().messages().lang(), play.mvc.Controller.request().attrs(), requestData(play.mvc.Controller.request()), requestFileData(play.mvc.Controller.request()), allowedFields);
    }

    @Override
    public DynamicForm bindFromRequest(Http.Request request, String... allowedFields) {
        return bind(this.messagesApi.preferred(request).lang(), request.attrs(), requestData(request), requestFileData(request), allowedFields);
    }

    @Override
    @Deprecated
    public DynamicForm bindFromRequest(Map<String,String[]> requestData, String... allowedFields) {
        return bindFromRequestData(ctxLang(), ctxRequestAttrs(), requestData, Collections.emptyMap(), allowedFields);
    }

    @Override
    public DynamicForm bindFromRequestData(Lang lang, TypedMap attrs, Map<String,String[]> requestData, Map<String, Http.MultipartFormData.FilePart<?>> requestFileData, String... allowedFields) {
        Map<String,String> data = new HashMap<>();
        fillDataWith(data, requestData);
        return bind(lang, attrs, data, requestFileData, allowedFields);
    }

    @Override
    @Deprecated
    public DynamicForm bind(JsonNode data, String... allowedFields) {
        return bind(ctxLang(), ctxRequestAttrs(), data, allowedFields);
    }

    @Override
    public DynamicForm bind(Lang lang, TypedMap attrs, JsonNode data, String... allowedFields) {
        return bind(lang, attrs,
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

    @Override
    @Deprecated
    public DynamicForm bind(Map<String,String> data, String... allowedFields) {
        return bind(ctxLang(), ctxRequestAttrs(), data, allowedFields);
    }

    @Override
    public DynamicForm bind(Lang lang, TypedMap attrs, Map<String,String> data, String... allowedFields) {
        return bind(lang, attrs, data, Collections.emptyMap(), allowedFields);
    }

    @Override
    public DynamicForm bind(Lang lang, TypedMap attrs, Map<String,String> data, Map<String, Http.MultipartFormData.FilePart<?>> files, String... allowedFields) {
        Form<Dynamic> form = super.bind(lang, attrs, data.entrySet().stream().collect(Collectors.toMap(e -> asDynamicKey(e.getKey()), e -> e.getValue())), files.entrySet().stream().collect(Collectors.toMap(e -> asDynamicKey(e.getKey()), e -> e.getValue())), allowedFields);
        return new DynamicForm(form.rawData(), form.files(), form.errors(), form.value(), messagesApi, formatters, validatorFactory, config, lang);
    }

    @Override
    public Form.Field field(String key, Lang lang) {
        // #1310: We specify inner class as Form.Field rather than Field because otherwise,
        // javadoc cannot find the static inner class.
        Field field = super.field(asDynamicKey(key), lang);
        return new Field(this, key, field.constraints(), field.format(), field.errors(),
            field.value().orElse(value(key).map(v -> v instanceof String ? (String)v : null).orElse(null)),
            field.file().orElse(value(key).map(v -> v instanceof Http.MultipartFormData.FilePart ? (Http.MultipartFormData.FilePart)v : null).orElse(null))
        );
    }

    @Override
    public Optional<ValidationError> error(String key) {
        return super.error(asDynamicKey(key));
    }

    @Override
    public DynamicForm withError(final ValidationError error) {
        final Form<Dynamic> form = super.withError(new ValidationError(asDynamicKey(error.key()), error.messages(), error.arguments()));
        return new DynamicForm(super.rawData(), super.files(), form.errors(), form.value(), this.messagesApi, this.formatters, this.validatorFactory, this.config, lang().orElse(null));
    }

    @Override
    public DynamicForm withError(final String key, final String error, final List<Object> args) {
        final Form<Dynamic> form = super.withError(asDynamicKey(key), error, args);
        return new DynamicForm(super.rawData(), super.files(), form.errors(), form.value(), this.messagesApi, this.formatters, this.validatorFactory, this.config, lang().orElse(null));
    }

    @Override
    public DynamicForm withError(final String key, final String error) {
        return withError(key, error, new ArrayList<>());
    }

    @Override
    public DynamicForm withGlobalError(final String error, final List<Object> args) {
        final Form<Dynamic> form = super.withGlobalError(error, args);
        return new DynamicForm(super.rawData(), super.files(), form.errors(), form.value(), this.messagesApi, this.formatters, this.validatorFactory, this.config, lang().orElse(null));
    }

    @Override
    public DynamicForm withGlobalError(final String error) {
        return withGlobalError(error, new ArrayList<>());
    }

    @Override
    public DynamicForm discardingErrors() {
        final Form<Dynamic> form = super.discardingErrors();
        return new DynamicForm(super.rawData(), super.files(), form.errors(), form.value(), this.messagesApi, this.formatters, this.validatorFactory, this.config, lang().orElse(null));
    }

    @Override
    public DynamicForm withLang(Lang lang) {
        return new DynamicForm(super.rawData(), super.files(), this.errors(), this.value(), this.messagesApi, this.formatters, this.validatorFactory, this.config, lang);
    }

    @Override
    public DynamicForm withDirectFieldAccess(boolean directFieldAccess) {
        if(!directFieldAccess) {
            // Just do nothing
            return this;
        }
        throw new RuntimeException("Not possible to enable direct field access for dynamic forms.");
    }

    // -- tools

    static String asDynamicKey(String key) {
        if(key.isEmpty() || MATCHES_DATA.matcher(key).matches()) {
           return key;
        } else {
            return "data[" + key + "]";
        }
    }

    static String asNormalKey(String key) {
        if(MATCHES_DATA.matcher(key).matches()) {
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

