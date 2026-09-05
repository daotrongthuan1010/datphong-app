package com.vivu.booking.utils;

import com.vivu.booking.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ValidationUtils {
    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private ValidationUtils() {
    }

    public static <T> void validate(T obj) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .toList();
            throw new ValidationException(errors);
        }
    }
    public static <T> void validates(T obj) {
        Set<ConstraintViolation<T>> violations  = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            Map<String,String> map=new HashMap<>();
            violations.stream()
                    .forEach(v->
                    {map.put(v.getPropertyPath().toString(),v.getMessage());
                    });
            throw new ValidationException(map);
        }
    }
}
