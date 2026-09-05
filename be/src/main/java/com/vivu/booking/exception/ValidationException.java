package com.vivu.booking.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends BusinessException {
    private  List<String> errors;
    private Map<String,String> errorMap;

    public ValidationException(List<String> errors) {
        super(422, "Validation failed");
        this.errors = errors;
    }
    public ValidationException(Map<String,String> errors) {
        super(422, "Validation failed");
        this.errorMap = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
    public Map<String, String> getErrorMap() {return errorMap;}
}
