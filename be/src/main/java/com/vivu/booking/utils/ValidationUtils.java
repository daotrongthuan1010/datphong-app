package com.vivu.booking.utils;

import com.vivu.booking.exception.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;

public final class ValidationUtils {
    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private ValidationUtils() {
    }

    public static <T> void validate(T obj) {
        var violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .toList();
            throw new ValidationException(errors);
        }
    }
}
