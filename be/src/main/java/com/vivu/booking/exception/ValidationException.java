package com.vivu.booking.exception;

import java.util.List;

public class ValidationException extends BusinessException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(422, "Validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
