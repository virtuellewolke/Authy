package de.reynok.authentication.core.shared.util.validation;

public interface Validator<T> {
    boolean isValid(T argument);

    default boolean isNotValid(T arg) {
        return !isValid(arg);
    }
}