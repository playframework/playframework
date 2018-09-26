/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ConstraintValidatorFactory to be used with compile-time Dependency Injection.
 */
public class MappedConstraintValidatorFactory implements ConstraintValidatorFactory {

    // This is a Map<Class, Supplier> so that we can have both
    // singletons and non-singletons validators.
    private final Map<Class<? extends ConstraintValidator>, Supplier<ConstraintValidator>> validators = new HashMap<>();

    /**
     * Adds validator as a singleton.
     *
     * @param key the constraint validator type
     * @param constraintValidator the constraint validator instance
     * @param <T> the type of constraint validator implementation
     * @return {@link MappedConstraintValidatorFactory} with the given constraint validator added.
     */
    public <T extends ConstraintValidator<?, ?>> MappedConstraintValidatorFactory addConstraintValidator(Class<T> key, T constraintValidator) {
        validators.put(key, () -> constraintValidator);
        return this;
    }

    /**
     * Adds validator as a non-singleton.
     *
     * @param key the constraint validator type
     * @param constraintValidator the constraint validator instance
     * @param <T> the type of constraint validator implementation
     * @return {@link MappedConstraintValidatorFactory} with the given constraint validator added.
     */
    public <T extends ConstraintValidator<?, ?>> MappedConstraintValidatorFactory addConstraintValidator(Class<T> key, Supplier<T> constraintValidator) {
        validators.put(key, constraintValidator::get);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return (T) validators.computeIfAbsent(key, clazz -> () -> newInstance(clazz)).get();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        validators.clear();
    }

    // This is a fallback to avoid that users needs to create every single
    // validator instance, which are usually very simple. We then create the
    // constraint validators automatically, even for compile-time dependency
    // injection, but we enable users to register their own instances if they
    // need to do so.
    private <T extends ConstraintValidator<?, ?>> T newInstance(Class<T> key) {
        try {
            return key.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | RuntimeException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
