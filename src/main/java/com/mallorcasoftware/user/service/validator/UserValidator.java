package com.mallorcasoftware.user.service.validator;

public interface UserValidator {
    void validatePassword(String password) throws IllegalArgumentException;
}
